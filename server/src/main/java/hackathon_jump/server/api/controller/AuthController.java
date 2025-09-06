package hackathon_jump.server.api.controller;

import hackathon_jump.server.business.service.auth.JwtService;
import hackathon_jump.server.business.service.auth.UserService;
import hackathon_jump.server.infrastructure.repository.IUserRepository;
import hackathon_jump.server.model.EOauthProvider;
import hackathon_jump.server.model.dto.Session;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private static Logger log = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private JwtService jwtService;

    @GetMapping("/oauth2/google/callback")
    public void handleGoogleCallback(
        @AuthenticationPrincipal OAuth2User oauth2User,
        @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient oAuth2AuthorizedClient,
        HttpServletRequest request,
        HttpServletResponse response
    ) throws IOException {
        log.debug("OAuth callback for {}", Optional.ofNullable(oauth2User.getAttribute("name")));

        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");

        if (email == null || email.isEmpty()) {
            response.sendRedirect("http://localhost:4200/auth/oauth2/callback?error=invalid_user");
            return;
        }
        // Get the access token from the OAuth2AuthorizedClient
        String accessToken = oAuth2AuthorizedClient.getAccessToken().getTokenValue();
        // todo add support for refresh tokens
        String refreshToken = oAuth2AuthorizedClient.getRefreshToken() == null ? "" :
                oAuth2AuthorizedClient.getRefreshToken().getTokenValue();
        userService.save(email, accessToken, EOauthProvider.GOOGLE);

        Map<String, Object> claims = Map.of(
            "googleEmailAddresses", List.of(email)
        );
        String jwt = jwtService.issue(email, claims);

        String redirectUrl = String.format(
            "http://localhost:4200/auth/oauth2/callback?token=%s&email=%s&name=%s&provider=%s",
            jwt, email, name, EOauthProvider.GOOGLE
        );
        response.sendRedirect(redirectUrl);
    }

    @GetMapping("/oauth2/google/linkedin")
    public void handleLinkedinCallback(
            @AuthenticationPrincipal OAuth2User oauth2User,
            @RegisteredOAuth2AuthorizedClient("linkedin") OAuth2AuthorizedClient oAuth2AuthorizedClient,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        log.debug("LinkedIn OAuth callback for user: {}", oauth2User.getName());
        log.debug("LinkedIn OAuth attributes: {}", oauth2User.getAttributes());

        // LinkedIn provides different attribute names
        String email = oauth2User.getAttribute("emailAddress");
        if (email == null) {
            email = oauth2User.getAttribute("email");
        }
        
        String firstName = oauth2User.getAttribute("firstName");
        String lastName = oauth2User.getAttribute("lastName");
        String name = "";
        
        if (firstName != null && lastName != null) {
            name = firstName + " " + lastName;
        } else if (firstName != null) {
            name = firstName;
        } else if (lastName != null) {
            name = lastName;
        } else {
            name = oauth2User.getName(); // fallback to the principal name
        }

        if (email == null || email.isEmpty()) {
            response.sendRedirect("http://localhost:4200/auth/oauth2/callback?error=invalid_user");
            return;
        }

        String accessToken = oAuth2AuthorizedClient.getAccessToken().getTokenValue();
        String refreshToken = oAuth2AuthorizedClient.getRefreshToken() == null ? "" :
                oAuth2AuthorizedClient.getRefreshToken().getTokenValue();

        userService.save(email, accessToken, EOauthProvider.LINKEDIN);

        Map<String, Object> claims = Map.of(
                "linkedinEmailAddresses", List.of(email)
        );
        String jwt = jwtService.issue(email, claims);

        String redirectUrl = String.format(
                "http://localhost:4200/auth/oauth2/callback?token=%s&username=%s&name=%s&provider=%s",
                jwt, email, name, EOauthProvider.LINKEDIN
        );
        response.sendRedirect(redirectUrl);
    }

    @PostMapping("/auth/tokens")
    public ResponseEntity<String> handleMergeTokens(@RequestAttribute("session") Session session,
                                            @RequestBody String token2) {
        Session session2 = jwtService.validateAndGetSession(token2);
        Session newSession = jwtService.mergeSessions(session, session2);
        String jwt = jwtService.issue(newSession);
        return ResponseEntity.ok(jwt);
    }
}
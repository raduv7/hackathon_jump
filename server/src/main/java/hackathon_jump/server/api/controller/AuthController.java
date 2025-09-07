package hackathon_jump.server.api.controller;

import hackathon_jump.server.business.service.auth.JwtService;
import hackathon_jump.server.business.service.auth.UserService;
import hackathon_jump.server.model.enums.EOauthProvider;
import hackathon_jump.server.model.dto.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
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
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private JwtService jwtService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

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
            response.sendRedirect(frontendUrl + "/auth/oauth2/callback?error=invalid_user");
            return;
        }
        // Get the access token from the OAuth2AuthorizedClient
//        String accessToken = oAuth2AuthorizedClient.getAccessToken().getTokenValue();
        String idToken = ((DefaultOidcUser) oauth2User).getIdToken().getTokenValue();
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
            "%s/auth/oauth2/callback?token=%s&email=%s&name=%s&provider=%s",
            frontendUrl, jwt, email, name, EOauthProvider.GOOGLE
        );
        response.sendRedirect(redirectUrl);
    }

    @GetMapping("/oauth2/linkedin/callback")
    public void handleLinkedinCallback(
            @AuthenticationPrincipal OAuth2User oauth2User,
            @RegisteredOAuth2AuthorizedClient("linkedin") OAuth2AuthorizedClient oAuth2AuthorizedClient,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        log.debug("LinkedIn OAuth callback for user: {}", oauth2User.getName());

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
            response.sendRedirect(frontendUrl + "/auth/oauth2/callback?error=invalid_user");
            return;
        }

        String accessToken = oAuth2AuthorizedClient.getAccessToken().getTokenValue();
        String refreshToken = oAuth2AuthorizedClient.getRefreshToken() == null ? "" :
                oAuth2AuthorizedClient.getRefreshToken().getTokenValue();

        userService.save(email, accessToken, EOauthProvider.LINKEDIN);

        Map<String, Object> claims = Map.of(
                "linkedinUsername", email
        );
        String jwt = jwtService.issue(email, claims);

        String redirectUrl = String.format(
                "%s/auth/oauth2/callback?token=%s&username=%s&name=%s&provider=%s",
                frontendUrl, jwt, email, name, EOauthProvider.LINKEDIN
        );
        response.sendRedirect(redirectUrl);
    }

    @GetMapping("/oauth2/facebook/callback")
    public void handleFacebookCallback(
            @AuthenticationPrincipal OAuth2User oauth2User,
            @RegisteredOAuth2AuthorizedClient("facebook") OAuth2AuthorizedClient oAuth2AuthorizedClient,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        log.debug("facebook OAuth callback for user: {}", oauth2User.getName());

        // Facebook provides different attribute names
        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");
        String username = oauth2User.getAttribute("username");
        
        // If no username, use email as username
        if (username == null || username.isEmpty()) {
            username = email;
        }
        
        // If no name, use username or email
        if (name == null || name.isEmpty()) {
            name = username != null ? username : email;
        }

        if (email == null || email.isEmpty()) {
            response.sendRedirect(frontendUrl + "/auth/oauth2/callback?error=invalid_user");
            return;
        }

        String accessToken = oAuth2AuthorizedClient.getAccessToken().getTokenValue();
        String refreshToken = oAuth2AuthorizedClient.getRefreshToken() == null ? "" :
                oAuth2AuthorizedClient.getRefreshToken().getTokenValue();

        userService.save(email, accessToken, EOauthProvider.FACEBOOK);

        Map<String, Object> claims = Map.of(
                "facebookUsername", email
        );
        String jwt = jwtService.issue(email, claims);

        String redirectUrl = String.format(
                "%s/auth/oauth2/callback?token=%s&username=%s&name=%s&provider=%s",
                frontendUrl, jwt, email, name, EOauthProvider.FACEBOOK
        );
        response.sendRedirect(redirectUrl);
    }

    @PostMapping("/tokens")
    public ResponseEntity<String> handleMergeTokens(@RequestAttribute("session") Session session,
                                            @RequestBody String token2) {
        Session session2 = jwtService.validateAndGetSession(token2);
        if(session2.getGoogleEmailAddresses() != null && !session2.getGoogleEmailAddresses().isEmpty()) {
            this.userService.syncUsers(session.getGoogleEmailAddresses().getFirst(), session2.getGoogleEmailAddresses().getFirst());
        }
        Session newSession = jwtService.mergeSessions(session, session2);
        String jwt = jwtService.issue(newSession);
        return ResponseEntity.ok(jwt);
    }

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pinged");
    }
}
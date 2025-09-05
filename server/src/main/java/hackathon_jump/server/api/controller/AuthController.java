package hackathon_jump.server.api.controller;

import hackathon_jump.server.business.service.auth.JwtService;
import hackathon_jump.server.model.EOauthProvider;
import hackathon_jump.server.model.dto.SignInResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    private JwtService jwtService;

    @GetMapping("/oauth2/google/callback")
    public ResponseEntity<SignInResponse> handleCallback(
        @AuthenticationPrincipal OAuth2User oauth2User,
        @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient client
    ) {
        log.debug("OAuth callback for {}", Optional.ofNullable(oauth2User.getAttribute("name")));
        
        // Extract user info from Google
        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");
        String picture = oauth2User.getAttribute("picture");
        
        // Get access token for Google Calendar API calls
        String accessToken = oauth2User.getAttribute("access_token");
        
        // Validate required fields
        if (email == null || email.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        // Generate JWT token with access token included
        Map<String, Object> claims = Map.of(
            "googleEmailAddresses", List.of(email)
        );
        String jwt = jwtService.issue(email, claims);
        
        // Return JWT to frontend
        return ResponseEntity.ok(new SignInResponse(jwt, email, name, picture, EOauthProvider.GOOGLE));
    }
}
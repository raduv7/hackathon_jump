package hackathon_jump.server.api.controller;

import hackathon_jump.server.business.service.auth.JwtService;
import hackathon_jump.server.model.EOauthProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    private JwtService jwtService;

    @GetMapping("/oauth2/google/callback")
    public void handleCallback(
        @AuthenticationPrincipal OAuth2User oauth2User,
        @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient client,
        HttpServletRequest request,
        HttpServletResponse response
    ) throws IOException {
        log.debug("OAuth callback for {}", Optional.ofNullable(oauth2User.getAttribute("name")));
        
        // Extract user info from Google
        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");
        
        // Get access token for Google Calendar API calls (stored for future use)
        // todo check this shit
         String accessToken = oauth2User.getAttribute("access_token");
        
        // Validate required fields
        if (email == null || email.isEmpty()) {
            response.sendRedirect("http://localhost:4200/auth/oauth2/callback?error=invalid_user");
            return;
        }
        
        // Generate JWT token with access token included
        Map<String, Object> claims = Map.of(
            "googleEmailAddresses", List.of(email)
        );
        String jwt = jwtService.issue(email, claims);
        
        // Redirect to frontend with JWT token and user info
        String redirectUrl = String.format(
            "http://localhost:4200/auth/oauth2/callback?token=%s&email=%s&name=%s&provider=%s",
            jwt, email, name, EOauthProvider.GOOGLE
        );
        response.sendRedirect(redirectUrl);
    }
}
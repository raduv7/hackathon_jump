package hackathon_jump.server.api.controller;

import hackathon_jump.server.business.service.auth.JwtService;
import hackathon_jump.server.model.JwtResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("")
public class AuthController {
    @Autowired
    private JwtService jwtService;
    
    @GetMapping("/oauth2/callback")
    public ResponseEntity<JwtResponse> handleCallback(
        @AuthenticationPrincipal OAuth2User oauth2User) {
        
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
            "name", name != null ? name : "",
            "picture", picture != null ? picture : "",
            "access_token", accessToken != null ? accessToken : ""
        );
        String jwt = jwtService.issue(email, claims);
        
        // Return JWT to frontend
        return ResponseEntity.ok(new JwtResponse(jwt, email, name, picture));
    }
}
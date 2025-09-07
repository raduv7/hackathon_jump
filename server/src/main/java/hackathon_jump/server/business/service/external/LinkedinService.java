package hackathon_jump.server.business.service.external;

import com.fasterxml.jackson.databind.ObjectMapper;
import hackathon_jump.server.infrastructure.repository.IUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class LinkedinService {
    @Autowired
    private IUserRepository userRepository;
    
    @Value("${app.linkedin.api-base-url}")
    private String linkedinApiBaseUrl;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    public LinkedinService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Post content to LinkedIn using the LinkedIn API
     * @param title The title of the post
     * @param text The content of the post
     * @param accessToken
     */
    public void post(String title, String text, String accessToken) {
        try {
            String userUrn = getLinkedInUserUrn(accessToken);
            if (userUrn == null) {
                log.error("Could not get LinkedIn URN");
                throw new RuntimeException("Could not get LinkedIn user information. Please reconnect your LinkedIn account.");
            }

            String postContent = createPostContent(title, text);
            String postId = createLinkedInPost(accessToken, userUrn, postContent);
            log.info("Successfully posted to LinkedIn. Post ID: {}", postId);
        } catch (Exception e) {
            log.error("Error posting to LinkedIn: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to post to LinkedIn: " + e.getMessage());
        }
    }
    
    /**
     * Get LinkedIn user URN (unique identifier)
     */
    private String getLinkedInUserUrn(String accessToken) {
        try {
            String url = linkedinApiBaseUrl + "/userinfo";
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);
            
            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url, HttpMethod.GET, requestEntity, (Class<Map<String, Object>>) (Class<?>) Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> userInfo = response.getBody();
                String sub = (String) userInfo.get("sub");
                if (sub != null) {
                    return "urn:li:person:" + sub;
                }
            }
            
            log.error("Failed to get LinkedIn user URN");
            return null;
            
        } catch (Exception e) {
            log.error("Error getting LinkedIn user URN: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Create the post content structure for LinkedIn API
     */
    private String createPostContent(String title, String text) {
        try {
            // Create the text content
            Map<String, Object> textContent = new HashMap<>();
            textContent.put("text", title + "\n\n" + text);
            
            // Create the post structure
            Map<String, Object> post = new HashMap<>();
            post.put("author", "urn:li:person:YOUR_USER_ID"); // This will be replaced with actual URN
            post.put("lifecycleState", "PUBLISHED");
            post.put("specificContent", Map.of(
                "com.linkedin.ugc.ShareContent", Map.of(
                    "shareCommentary", textContent,
                    "shareMediaCategory", "NONE"
                )
            ));
            post.put("visibility", Map.of(
                "com.linkedin.ugc.MemberNetworkVisibility", "PUBLIC"
            ));
            
            return objectMapper.writeValueAsString(post);
            
        } catch (Exception e) {
            log.error("Error creating post content: {}", e.getMessage());
            throw new RuntimeException("Failed to create post content");
        }
    }
    
    /**
     * Create the actual LinkedIn post
     */
    private String createLinkedInPost(String accessToken, String userUrn, String postContent) {
        try {
            String url = linkedinApiBaseUrl + "/ugcPosts";
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Restli-Protocol-Version", "2.0.0");
            
            // Replace placeholder with actual user URN
            String finalPostContent = postContent.replace("urn:li:person:YOUR_USER_ID", userUrn);
            
            HttpEntity<String> requestEntity = new HttpEntity<>(finalPostContent, headers);
            
            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url, HttpMethod.POST, requestEntity, (Class<Map<String, Object>>) (Class<?>) Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                String postId = (String) responseBody.get("id");
                log.info("LinkedIn post created successfully with ID: {}", postId);
                return postId;
            } else {
                log.error("Failed to create LinkedIn post. Status: {}, Body: {}", 
                    response.getStatusCode(), response.getBody());
                throw new RuntimeException("Failed to create LinkedIn post");
            }
            
        } catch (Exception e) {
            log.error("Error creating LinkedIn post: {}", e.getMessage());
            throw new RuntimeException("Failed to create LinkedIn post: " + e.getMessage());
        }
    }
    
    /**
     * Simplified post method for backward compatibility
     * Note: This method will not work without user context
     */
    public void post(String title, String text) {
        log.warn("LinkedIn post called without user context. This will not work.");
        throw new RuntimeException("LinkedIn posting requires user context. Use post(title, text, userEmail) instead.");
    }
}

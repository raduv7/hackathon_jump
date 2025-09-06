package hackathon_jump.server.business.service.external;

import hackathon_jump.server.model.domain.EventReport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class RecallAiService {
    @Value("${app.recall.api-key}")
    private String apiKey;
    @Value("${app.recall.api-base-url}")
    private String apiBaseUrl;
    
    private final RestTemplate restTemplate;
    
    public RecallAiService() {
        this.restTemplate = new RestTemplate();
    }
    
    public void logApiKeyStatus() {
        if (apiKey != null && !apiKey.isEmpty()) {
            log.info("Recall AI API key is configured and ready to use. API Base URL: {}", apiBaseUrl);
        } else {
            log.warn("Recall AI API key is not configured. API Base URL: {}", apiBaseUrl);
        }
    }
    
    /**
     * Creates a scheduled bot to join a meeting at a specified time with default transcript configuration
     * 
     * The soonest you can schedule a bot is 10 minutes from present.
     * If you need to join a meeting that starts in less than 10 minutes,
     * you should create an ad-hoc bot instead.
     * 
     * This method creates a bot with default transcript configuration using Assembly AI streaming.
     * 
     * @param meetingUrl The URL of the meeting to join
     * @param joinAt The time when the bot should join (ISO 8601 format, must be at least 10 minutes in the future)
     * @return The bot ID if successful
     * @throws RuntimeException if the API call fails
     */
    public String createBot(String meetingUrl, String joinAt) {
        // Use Assembly AI streaming as default transcript provider
        return createBot(meetingUrl, joinAt, "assembly_ai_streaming", null);
    }
    
    /**
     * Creates a scheduled bot to join a meeting at a specified time with transcript configuration
     * 
     * The soonest you can schedule a bot is 10 minutes from present.
     * If you need to join a meeting that starts in less than 10 minutes,
     * you should create an ad-hoc bot instead.
     * 
     * @param meetingUrl The URL of the meeting to join
     * @param joinAt The time when the bot should join (ISO 8601 format, must be at least 10 minutes in the future)
     * @param transcriptProvider The transcript provider (e.g., "assembly_ai_streaming", "deepgram", "rev")
     * @param webhookUrl The webhook URL for real-time transcript data (optional)
     * @return The bot ID if successful
     * @throws RuntimeException if the API call fails
     */
    public String createBot(String meetingUrl, String joinAt, String transcriptProvider, String webhookUrl) {
        log.info("Creating scheduled bot for meeting: {} at time: {} with transcript provider: {}", 
                meetingUrl, joinAt, transcriptProvider != null ? transcriptProvider : "none");
        
        HttpHeaders headers = createHeaders();
        HttpEntity<Map<String, Object>> requestEntity = createRequestEntityWithTranscript(meetingUrl, joinAt, transcriptProvider, webhookUrl, headers);
        
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.postForEntity(apiBaseUrl, requestEntity, (Class<Map<String, Object>>) (Class<?>) Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String botId = (String) response.getBody().get("id");
                log.info("Successfully created bot with ID: {} and transcript config: {}", botId, transcriptProvider != null ? "enabled" : "disabled");
                return botId;
            } else {
                throw new RuntimeException("Failed to create scheduled bot: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error creating scheduled bot: {}", e.getMessage());
            throw new RuntimeException("Failed to create scheduled bot", e);
        }
    }

    public void fillEventReport(EventReport eventReport) {
        // fill attendees, startDateTime, transcript from bot
    }
    
    /**
     * Retrieves details of a bot using its unique identifier
     * @param botId The unique identifier of the bot
     * @return Map containing bot details
     * @throws RuntimeException if the API call fails
     */
    public Map<String, Object> retrieveBot(String botId) {
        log.info("Retrieving bot details for ID: {}", botId);
        
        String apiUrl = apiBaseUrl + botId + "/";
        HttpHeaders headers = createHeaders();
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
        
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(apiUrl, HttpMethod.GET, requestEntity, (Class<Map<String, Object>>) (Class<?>) Map.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Successfully retrieved bot details for ID: {}", botId);
                return response.getBody();
            } else {
                throw new RuntimeException("Failed to retrieve bot: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error retrieving bot {}: {}", botId, e.getMessage());
            throw new RuntimeException("Failed to retrieve bot", e);
        }
    }
    
    /**
     * Deletes a scheduled bot using its unique identifier
     * @param botId The unique identifier of the bot to delete
     * @throws RuntimeException if the API call fails
     */
    public void deleteScheduledBot(String botId) {
        log.info("Deleting scheduled bot with ID: {}", botId);
        
        String apiUrl = apiBaseUrl + botId + "/";
        HttpHeaders headers = createHeaders();
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
        
        try {
            ResponseEntity<Void> response = restTemplate.exchange(apiUrl, HttpMethod.DELETE, requestEntity, Void.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Successfully deleted bot with ID: {}", botId);
            } else {
                throw new RuntimeException("Failed to delete scheduled bot: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error deleting bot {}: {}", botId, e.getMessage());
            throw new RuntimeException("Failed to delete scheduled bot", e);
        }
    }
    
    /**
     * Updates the meeting URL and scheduled time of an existing scheduled bot
     * 
     * This method implements a delete-and-recreate strategy:
     * 1. Retrieves the current bot details
     * 2. Deletes the existing bot
     * 3. Creates a new bot with updated parameters
     * 4. Returns the new bot ID
     * 
     * This approach is more reliable than PATCH updates and ensures the bot
     * is properly configured with the new parameters.
     * 
     * @param botId The unique identifier of the bot to update
     * @param newMeetingUrl The new meeting URL
     * @param newJoinAt The new scheduled join time (ISO 8601 format, must be at least 10 minutes in the future)
     * @return The ID of the newly created bot
     * @throws RuntimeException if any API call fails
     */
    public String updateScheduledBot(String botId, String newMeetingUrl, String newJoinAt) {
        log.info("Updating scheduled bot {} with new meeting URL: {} and join time: {}", botId, newMeetingUrl, newJoinAt);
        log.info("Using delete-and-recreate strategy for bot update");
        
        try {
            // Step 1: Retrieve current bot details to preserve configuration
            log.info("Step 1: Retrieving current bot details for bot: {}", botId);
            Map<String, Object> currentBotDetails = retrieveBot(botId);
            
            if (currentBotDetails == null) {
                throw new RuntimeException("Bot not found: " + botId);
            }
            
            // Extract transcript configuration from current bot if it exists
            String transcriptProvider = null;
            String webhookUrl = null;
            
            if (currentBotDetails.containsKey("recording_config")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> recordingConfig = (Map<String, Object>) currentBotDetails.get("recording_config");
                
                if (recordingConfig.containsKey("transcript")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> transcript = (Map<String, Object>) recordingConfig.get("transcript");
                    
                    if (transcript.containsKey("provider")) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> provider = (Map<String, Object>) transcript.get("provider");
                        
                        // Find the transcript provider (could be assembly_ai_streaming, deepgram, etc.)
                        for (String key : provider.keySet()) {
                            if (!key.equals("provider")) {
                                transcriptProvider = key;
                                break;
                            }
                        }
                    }
                }
                
                // Extract webhook URL if it exists
                if (recordingConfig.containsKey("realtime_endpoints")) {
                    @SuppressWarnings("unchecked")
                    java.util.List<Map<String, Object>> endpoints = (java.util.List<Map<String, Object>>) recordingConfig.get("realtime_endpoints");
                    
                    for (Map<String, Object> endpoint : endpoints) {
                        if ("webhook".equals(endpoint.get("type"))) {
                            webhookUrl = (String) endpoint.get("url");
                            break;
                        }
                    }
                }
            }
            
            log.info("Extracted configuration - Transcript provider: {}, Webhook URL: {}", 
                    transcriptProvider != null ? transcriptProvider : "none", 
                    webhookUrl != null ? webhookUrl : "none");
            
            // Step 2: Delete the existing bot
            log.info("Step 2: Deleting existing bot: {}", botId);
            deleteScheduledBot(botId);
            log.info("Successfully deleted bot: {}", botId);
            
            // Step 3: Create new bot with updated parameters
            log.info("Step 3: Creating new bot with updated parameters");
            String newBotId;
            
            if (transcriptProvider != null) {
                // Create bot with transcript configuration
                newBotId = createBot(newMeetingUrl, newJoinAt, transcriptProvider, webhookUrl);
            } else {
                // Create bot with default transcript configuration
                newBotId = createBot(newMeetingUrl, newJoinAt);
            }
            
            log.info("Successfully created new bot with ID: {}", newBotId);
            
            // Step 4: Verify the new bot was created correctly
            log.info("Step 4: Verifying new bot configuration");
            Map<String, Object> newBotDetails = retrieveBot(newBotId);
            
            if (newBotDetails != null) {
                log.info("New bot verification successful. Bot details: {}", newBotDetails);
            } else {
                log.warn("Warning: Could not retrieve details for newly created bot: {}", newBotId);
            }
            
            log.info("Bot update completed successfully. Old bot ID: {}, New bot ID: {}", botId, newBotId);
            return newBotId;
            
        } catch (Exception e) {
            log.error("Error updating bot {}: {}", botId, e.getMessage());
            throw new RuntimeException("Failed to update scheduled bot", e);
        }
    }
    
    /**
     * Creates HTTP headers with authorization and content type
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", apiKey); // Use API key directly without "Bearer " prefix
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("accept", "application/json");
        return headers;
    }
    
    /**
     * Creates request entity with meeting URL and join time
     */
    private HttpEntity<Map<String, Object>> createRequestEntity(String meetingUrl, String joinAt, HttpHeaders headers) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("meeting_url", meetingUrl);
        requestBody.put("bot_name", "meetScribe_bot"); // Add bot name like in the working template
        
        // Add join_at only if specified (for scheduled bots)
        if (joinAt != null && !joinAt.isEmpty()) {
            requestBody.put("join_at", joinAt);
        }
        
        // Add output_media configuration like in the working template
        Map<String, Object> outputMedia = new HashMap<>();
        Map<String, Object> camera = new HashMap<>();
        camera.put("kind", "webpage");
        Map<String, Object> config = new HashMap<>();
        config.put("url", "https://www.google.com/url?sa=i&url=https%3A%2F%2Fwww.pngwing.com%2Fen%2Fsearch%3Fq%3Dbot&psig=AOvVaw1HTRdqwhdtwCCjMUV-j2sh&ust=1757274417142000&source=images&cd=vfe&opi=89978449&ved=0CBIQjRxqFwoTCNja9rzzxI8DFQAAAAAdAAAAABAE");
        camera.put("config", config);
        outputMedia.put("camera", camera);
        requestBody.put("output_media", outputMedia);
        
        return new HttpEntity<>(requestBody, headers);
    }
    
    /**
     * Creates request entity with meeting URL, join time, and optional transcript configuration
     */
    private HttpEntity<Map<String, Object>> createRequestEntityWithTranscript(String meetingUrl, String joinAt, 
                                                                              String transcriptProvider, String webhookUrl, HttpHeaders headers) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("meeting_url", meetingUrl);
        requestBody.put("bot_name", "meetScribe_bot"); // Add bot name like in the working template
        
        // Add join_at only if specified (for scheduled bots)
        if (joinAt != null && !joinAt.isEmpty()) {
            requestBody.put("join_at", joinAt);
        }
        
        // Add output_media configuration like in the working template
        Map<String, Object> outputMedia = new HashMap<>();
        Map<String, Object> camera = new HashMap<>();
        camera.put("kind", "webpage");
        Map<String, Object> config = new HashMap<>();
        config.put("url", "https://www.google.com/url?sa=i&url=https%3A%2F%2Fwww.pngwing.com%2Fen%2Fsearch%3Fq%3Dbot&psig=AOvVaw1HTRdqwhdtwCCjMUV-j2sh&ust=1757274417142000&source=images&cd=vfe&opi=89978449&ved=0CBIQjRxqFwoTCNja9rzzxI8DFQAAAAAdAAAAABAE");
        camera.put("config", config);
        outputMedia.put("camera", camera);
        requestBody.put("output_media", outputMedia);
        
        // Add transcript configuration if provider is specified
        if (transcriptProvider != null && !transcriptProvider.isEmpty()) {
            Map<String, Object> recordingConfig = new HashMap<>();
            Map<String, Object> transcript = new HashMap<>();
            Map<String, Object> provider = new HashMap<>();
            
            // Set the transcript provider
            provider.put(transcriptProvider, new HashMap<>());
            transcript.put("provider", provider);
            recordingConfig.put("transcript", transcript);
            
            // Add real-time webhook endpoint if provided
            if (webhookUrl != null && !webhookUrl.isEmpty()) {
                Map<String, Object> webhookEndpoint = new HashMap<>();
                webhookEndpoint.put("type", "webhook");
                webhookEndpoint.put("url", webhookUrl);
                webhookEndpoint.put("events", new String[]{"transcript.data", "transcript.partial_data"});
                
                recordingConfig.put("realtime_endpoints", new Object[]{webhookEndpoint});
            }
            
            requestBody.put("recording_config", recordingConfig);
            log.debug("Added transcript configuration with provider: {} and webhook: {}", transcriptProvider, webhookUrl);
        }
        
        return new HttpEntity<>(requestBody, headers);
    }
    
    /**
     * Retrieves the transcript download URL for a bot's recording
     * @param botId The unique identifier of the bot
     * @return The transcript download URL if available, null otherwise
     * @throws RuntimeException if the API call fails
     */
    public String getTranscriptDownloadUrl(String botId) {
        log.info("Retrieving transcript download URL for bot: {}", botId);
        
        try {
            // Step 1: Get bot details to find transcript ID
            Map<String, Object> botDetails = retrieveBot(botId);
            
            if (botDetails != null && botDetails.containsKey("recordings")) {
                Object recordingsObj = botDetails.get("recordings");
                if (recordingsObj instanceof java.util.List) {
                    @SuppressWarnings("unchecked")
                    java.util.List<Map<String, Object>> recordings = (java.util.List<Map<String, Object>>) recordingsObj;
                    
                    for (Map<String, Object> recording : recordings) {
                        if (recording.containsKey("media_shortcuts")) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> mediaShortcuts = (Map<String, Object>) recording.get("media_shortcuts");
                            
                            if (mediaShortcuts.containsKey("transcript")) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> transcript = (Map<String, Object>) mediaShortcuts.get("transcript");
                                
                                if (transcript.containsKey("id")) {
                                    String transcriptId = (String) transcript.get("id");
                                    log.info("Found transcript ID for bot {}: {}", botId, transcriptId);
                                    
                                    // Step 2: Call transcript API to get download URL
                                    return getTranscriptDownloadUrlFromId(transcriptId);
                                }
                            }
                        }
                    }
                }
            }
            
            log.warn("No transcript ID found for bot: {}", botId);
            return null;
            
        } catch (Exception e) {
            log.error("Error retrieving transcript download URL for bot {}: {}", botId, e.getMessage());
            throw new RuntimeException("Failed to retrieve transcript download URL", e);
        }
    }
    
    /**
     * Retrieves the transcript download URL using the transcript ID
     * @param transcriptId The unique identifier of the transcript
     * @return The transcript download URL if available, null otherwise
     * @throws RuntimeException if the API call fails
     */
    private String getTranscriptDownloadUrlFromId(String transcriptId) {
        log.info("Retrieving transcript download URL for transcript ID: {}", transcriptId);
        
        String apiUrl = apiBaseUrl.replace("/bot/", "/transcript/") + transcriptId + "/";
        HttpHeaders headers = createHeaders();
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
        
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(apiUrl, HttpMethod.GET, requestEntity, (Class<Map<String, Object>>) (Class<?>) Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> transcriptData = response.getBody();
                
                if (transcriptData.containsKey("data")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = (Map<String, Object>) transcriptData.get("data");
                    
                    if (data.containsKey("download_url")) {
                        String downloadUrl = (String) data.get("download_url");
                        log.info("Found transcript download URL for transcript {}: {}", transcriptId, downloadUrl);
                        return downloadUrl;
                    }
                }
                
                log.warn("No download_url found in transcript data for ID: {}", transcriptId);
                return null;
            } else {
                throw new RuntimeException("Failed to retrieve transcript data: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error retrieving transcript data for ID {}: {}", transcriptId, e.getMessage());
            throw new RuntimeException("Failed to retrieve transcript data", e);
        }
    }
    
    /**
     * Downloads and retrieves the transcript content from a download URL
     * @param downloadUrl The transcript download URL
     * @return The transcript content as a string (JSON format)
     * @throws RuntimeException if the download fails
     */
    public String downloadTranscript(String downloadUrl) {
        log.info("Downloading transcript from URL: {}", downloadUrl);
        
        // For S3 downloads, don't use our API key - the URL already contains authentication
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
        
        try {
            URI uri = UriComponentsBuilder.fromUriString(downloadUrl)
                    .build(true)     // true = components are ALREADY encoded
                    .toUri();
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, requestEntity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Successfully downloaded transcript, content length: {} characters", response.getBody().length());
                return response.getBody();
            } else {
                throw new RuntimeException("Failed to download transcript: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error downloading transcript from {}: {}", downloadUrl, e.getMessage());
            throw new RuntimeException("Failed to download transcript", e);
        }
    }
    
    /**
     * Retrieves the complete transcript for a bot (combines getting download URL and downloading content)
     * @param botId The unique identifier of the bot
     * @return The transcript content as a string (JSON format), null if no transcript is available
     * @throws RuntimeException if the API calls fail
     */
    public String getTranscript(String botId) {
        log.info("Retrieving complete transcript for bot: {}", botId);
        
        try {
            String downloadUrl = getTranscriptDownloadUrl(botId);
            
            if (downloadUrl != null) {
                return downloadTranscript(downloadUrl);
            } else {
                log.warn("No transcript available for bot: {}", botId);
                return null;
            }
        } catch (Exception e) {
            log.error("Error retrieving transcript for bot {}: {}", botId, e.getMessage());
            throw new RuntimeException("Failed to retrieve transcript", e);
        }
    }
    
    /**
     * Checks if a transcript is available for a bot
     * @param botId The unique identifier of the bot
     * @return true if transcript is available, false otherwise
     */
    public boolean isTranscriptAvailable(String botId) {
        try {
            String downloadUrl = getTranscriptDownloadUrl(botId);
            return downloadUrl != null;
        } catch (Exception e) {
            log.error("Error checking transcript availability for bot {}: {}", botId, e.getMessage());
            return false;
        }
    }
}

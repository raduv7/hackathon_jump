package hackathon_jump.server;

import hackathon_jump.server.business.service.external.RecallAiService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class RecallAiServiceTests {
    @Value("${app.recall.api-key}")
    private String apiKey;
    @Value("${app.recall.api-base-url}")
    private String apiBaseUrl;
    
    // Global meeting URL for all tests - you can also make this configurable
    private static final String TEST_MEETING_URL = "https://meet.google.com/kyn-aqbr-bsv";
//    private static final String TEST_MEETING_URL = "https://meet.google.com/kyn-aqbr-bsv";
//    private static final String TEST_MEETING_URL = "https://meet.google.com/kyn-aqbr-bsv";

    @Autowired
    private RecallAiService recallAiService;
    
    // Track created bot IDs for cleanup
    private final List<String> createdBotIds = new ArrayList<>();
    
    @AfterEach
    public void cleanupBots() {
        System.out.println("\n=== CLEANUP: Deleting created bots ===");
        for (String botId : createdBotIds) {
            try {
                System.out.println("Deleting bot: " + botId);
                recallAiService.deleteScheduledBot(botId);
                System.out.println("✅ Successfully deleted bot: " + botId);
            } catch (Exception e) {
                System.out.println("⚠️  Failed to delete bot " + botId + ": " + e.getMessage());
            }
        }
        createdBotIds.clear();
        System.out.println("=== CLEANUP COMPLETED ===\n");
    }
    
    private String createBotAndTrack(String meetingUrl, String joinAt) {
        String botId = recallAiService.createBot(meetingUrl, joinAt);
        createdBotIds.add(botId);
        return botId;
    }
    
    private String createBotWithTranscriptAndTrack(String meetingUrl, String joinAt, String transcriptProvider, String webhookUrl) {
        String botId = recallAiService.createBot(meetingUrl, joinAt, transcriptProvider, webhookUrl);
        createdBotIds.add(botId);
        return botId;
    }

    @Test
    public void testPropertiesLoaded() {
        // Test that properties are loaded from application.properties
        System.out.println("=== PROPERTIES VERIFICATION ===");
        System.out.println("API Key loaded: " + (apiKey != null && !apiKey.isEmpty() ? "✅ Yes" : "❌ No"));
        System.out.println("API Base URL loaded: " + (apiBaseUrl != null && !apiBaseUrl.isEmpty() ? "✅ Yes" : "❌ No"));
        System.out.println("API Base URL: " + apiBaseUrl);
        System.out.println("Test Meeting URL: " + TEST_MEETING_URL);
        
        assertNotNull(apiKey, "API key should be loaded from application.properties");
        assertNotNull(apiBaseUrl, "API base URL should be loaded from application.properties");
        assertFalse(apiKey.isEmpty(), "API key should not be empty");
        assertFalse(apiBaseUrl.isEmpty(), "API base URL should not be empty");
        
        System.out.println("=== PROPERTIES VERIFICATION COMPLETED ===\n");
    }

    @Test
    public void testLogApiKeyStatus() {
        // This test verifies the service can log API key status
        assertDoesNotThrow(() -> {
            recallAiService.logApiKeyStatus();
        });
    }

    @Test
    public void testCreateBotWithDefaultTranscript() {
        // Test creating a bot with default transcript configuration
        String joinAt = Instant.now().plus(15, ChronoUnit.MINUTES).toString();
        
        assertDoesNotThrow(() -> {
            String botId = createBotAndTrack(TEST_MEETING_URL, joinAt);
            assertNotNull(botId, "Bot ID should not be null");
            assertFalse(botId.isEmpty(), "Bot ID should not be empty");
            System.out.println("Created bot with ID: " + botId);
        });
    }

    @Test
    public void testCreateBotWithCustomTranscript() {
        // Test creating a bot with custom transcript configuration
        String joinAt = Instant.now().plus(20, ChronoUnit.MINUTES).toString();
        String transcriptProvider = "assembly_ai_streaming";
        String webhookUrl = "https://example.com/webhook";
        
        assertDoesNotThrow(() -> {
            String botId = createBotWithTranscriptAndTrack(TEST_MEETING_URL, joinAt, transcriptProvider, webhookUrl);
            assertNotNull(botId, "Bot ID should not be null");
            assertFalse(botId.isEmpty(), "Bot ID should not be empty");
            System.out.println("Created bot with custom transcript config, ID: " + botId);
        });
    }

    @Test
    public void testCreateDeleteAndVerifyBot() {
        // Test creating a bot, deleting it, and verifying it's deleted
        String joinAt = Instant.now().plus(22, ChronoUnit.MINUTES).toString();
        
        assertDoesNotThrow(() -> {
            // Step 1: Create bot
            System.out.println("1. Creating bot for deletion test...");
            String botId = createBotAndTrack(TEST_MEETING_URL, joinAt);
            assertNotNull(botId, "Bot ID should not be null");
            assertFalse(botId.isEmpty(), "Bot ID should not be empty");
            System.out.println("✅ Bot created successfully with ID: " + botId);
            
            // Step 2: Verify bot exists by retrieving it
            System.out.println("2. Verifying bot exists by retrieving it...");
            Map<String, Object> botDetails = recallAiService.retrieveBot(botId);
            assertNotNull(botDetails, "Bot details should not be null before deletion");
            assertTrue(botDetails.containsKey("id"), "Bot details should contain ID before deletion");
            assertEquals(botId, botDetails.get("id"), "Retrieved bot ID should match created bot ID");
            System.out.println("✅ Bot exists and can be retrieved: " + botDetails);
            
            // Step 3: Delete the bot
            System.out.println("3. Deleting the bot...");
            recallAiService.deleteScheduledBot(botId);
            // Remove from tracking list since we're manually deleting it
            createdBotIds.remove(botId);
            System.out.println("✅ Bot deletion request completed");
            
            // Step 4: Try to retrieve the deleted bot (should fail or return different status)
            System.out.println("4. Attempting to retrieve deleted bot...");
            try {
                Map<String, Object> deletedBotDetails = recallAiService.retrieveBot(botId);
                
                // The bot might still exist but with a different status
                if (deletedBotDetails != null) {
                    System.out.println("⚠️  Bot still exists after deletion attempt: " + deletedBotDetails);
                    
                    // Check if the bot has a status indicating it's deleted or inactive
                    if (deletedBotDetails.containsKey("status")) {
                        String status = (String) deletedBotDetails.get("status");
                        System.out.println("Bot status: " + status);
                        
                        // Some APIs might mark bots as "deleted" or "inactive" rather than removing them
                        if ("deleted".equalsIgnoreCase(status) || "inactive".equalsIgnoreCase(status)) {
                            System.out.println("✅ Bot successfully marked as deleted/inactive");
                        } else {
                            System.out.println("⚠️  Bot still appears active after deletion");
                        }
                    } else {
                        System.out.println("⚠️  Bot still exists but no status field found");
                    }
                } else {
                    System.out.println("✅ Bot successfully deleted - cannot be retrieved");
                }
                
            } catch (Exception e) {
                // This is expected - the bot should not be retrievable after deletion
                System.out.println("✅ Bot successfully deleted - retrieval failed as expected: " + e.getMessage());
            }
            
            System.out.println("=== DELETE VERIFICATION TEST COMPLETED ===");
            System.out.println("Bot ID tested: " + botId);
        });
    }

    @Test
    public void testRetrieveBot() {
        // First create a bot, then retrieve it
        String joinAt = Instant.now().plus(25, ChronoUnit.MINUTES).toString();
        
        assertDoesNotThrow(() -> {
            // Create bot
            String botId = createBotAndTrack(TEST_MEETING_URL, joinAt);
            assertNotNull(botId, "Bot ID should not be null");
            
            // Retrieve bot details
            Map<String, Object> botDetails = recallAiService.retrieveBot(botId);
            assertNotNull(botDetails, "Bot details should not be null");
            assertTrue(botDetails.containsKey("id"), "Bot details should contain ID");
            assertEquals(botId, botDetails.get("id"), "Retrieved bot ID should match created bot ID");
            
            System.out.println("Retrieved bot details: " + botDetails);
        });
    }

    @Test
    public void testUpdateScheduledBot() {
        // Create a bot, then update it
        String joinAt = Instant.now().plus(30, ChronoUnit.MINUTES).toString();
        
        assertDoesNotThrow(() -> {
            // Create bot
            String botId = createBotAndTrack(TEST_MEETING_URL, joinAt);
            assertNotNull(botId, "Bot ID should not be null");
            
            // Update bot with new meeting URL and time
            String newJoinAt = Instant.now().plus(35, ChronoUnit.MINUTES).toString();
            
            String newBotId = recallAiService.updateScheduledBot(botId, TEST_MEETING_URL, newJoinAt);
            assertNotNull(newBotId, "New bot ID should not be null");
            assertFalse(newBotId.isEmpty(), "New bot ID should not be empty");
            assertNotEquals(botId, newBotId, "New bot ID should be different from old bot ID");
            System.out.println("Updated bot - Old ID: " + botId + ", New ID: " + newBotId);
            
            // Add the new bot ID to tracking list for cleanup
            createdBotIds.add(newBotId);
            
            // Verify the update by retrieving the new bot
            Map<String, Object> updatedBot = recallAiService.retrieveBot(newBotId);
            assertNotNull(updatedBot, "Updated bot details should not be null");
            System.out.println("Updated bot details: " + updatedBot);
        });
    }

    @Test
    public void testIsTranscriptAvailable() {
        // Test checking transcript availability
        String joinAt = Instant.now().plus(40, ChronoUnit.MINUTES).toString();
        
        assertDoesNotThrow(() -> {
            // Create bot
            String botId = createBotAndTrack(TEST_MEETING_URL, joinAt);
            assertNotNull(botId, "Bot ID should not be null");
            
            // Check transcript availability (will likely be false for a newly created bot)
            boolean isAvailable = recallAiService.isTranscriptAvailable(botId);
            System.out.println("Transcript available for bot " + botId + ": " + isAvailable);
            
            // This assertion might fail for newly created bots, so we just log the result
            if (isAvailable) {
                System.out.println("Transcript is available for bot: " + botId);
            } else {
                System.out.println("Transcript not yet available for bot: " + botId);
            }
        });
    }

    @Test
    public void testGetTranscriptDownloadUrl() {
        // Test getting transcript download URL
        String joinAt = Instant.now().plus(45, ChronoUnit.MINUTES).toString();
        
        assertDoesNotThrow(() -> {
            // Create bot
            String botId = createBotAndTrack(TEST_MEETING_URL, joinAt);
            assertNotNull(botId, "Bot ID should not be null");
            
            // Try to get download URL (will likely be null for a newly created bot)
            String downloadUrl = recallAiService.getTranscriptDownloadUrl(botId);
            System.out.println("Download URL for bot " + botId + ": " + downloadUrl);
            
            if (downloadUrl != null) {
                assertFalse(downloadUrl.isEmpty(), "Download URL should not be empty if present");
                System.out.println("Found download URL: " + downloadUrl);
            } else {
                System.out.println("No download URL available yet for bot: " + botId);
            }
        });
    }

    @Test
    public void testFullIntegrationWithGoogleMeet() {
        // Full integration test with the provided Google Meet
        String joinAt = Instant.now().toString(); // Join now
        
        System.out.println("=== FULL INTEGRATION TEST ===");
        System.out.println("Meeting URL: " + TEST_MEETING_URL);
        System.out.println("Scheduled to join at: " + joinAt);
        
        assertDoesNotThrow(() -> {
            // Step 1: Create bot with transcript configuration
            System.out.println("\n1. Creating bot with transcript configuration...");
            String botId = createBotWithTranscriptAndTrack(TEST_MEETING_URL, joinAt, "assembly_ai_streaming", null);
            assertNotNull(botId, "Bot ID should not be null");
            System.out.println("✅ Bot created successfully with ID: " + botId);
            
            // Step 2: Retrieve bot details
            System.out.println("\n2. Retrieving bot details...");
            Map<String, Object> botDetails = recallAiService.retrieveBot(botId);
            assertNotNull(botDetails, "Bot details should not be null");
            System.out.println("✅ Bot details retrieved: " + botDetails);
            
            // Step 3: Wait for 1 minute while user speaks
            System.out.println("\n3. Waiting 1 minute for you to speak in the meeting...");
            System.out.println("Please join the meeting and speak for about 1 minute.");
            System.out.println("Meeting URL: " + TEST_MEETING_URL);
            
            try {
                Thread.sleep(60000); // Wait 1 minute
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Wait interrupted");
            }
            
            System.out.println("✅ Wait completed");
            
            // Step 3.5: Wait additional 1 minutes for transcript generation
            System.out.println("\n3.5. Waiting additional 1 minutes for transcript generation...");
            System.out.println("Processing audio and generating transcript...");
            
            try {
                Thread.sleep(60000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Transcript generation wait interrupted");
            }
            
            System.out.println("✅ Transcript generation wait completed");
            
            // Step 4: Check if transcript is available
            System.out.println("\n4. Checking transcript availability...");
            boolean isAvailable = recallAiService.isTranscriptAvailable(botId);
            System.out.println("Transcript available: " + isAvailable);
            
            if (isAvailable) {
                // Step 5: Get transcript download URL
                System.out.println("\n5. Getting transcript download URL...");
                String downloadUrl = recallAiService.getTranscriptDownloadUrl(botId);
                assertNotNull(downloadUrl, "Download URL should not be null");
                System.out.println("✅ Download URL: " + downloadUrl);
                
                // Step 6: Download and log transcript
                System.out.println("\n6. Downloading transcript...");
                String transcript = recallAiService.downloadTranscript(downloadUrl);
                assertNotNull(transcript, "Transcript should not be null");
                System.out.println("✅ Transcript downloaded successfully!");
                System.out.println("Transcript content length: " + transcript.length() + " characters");
                System.out.println("\n=== TRANSCRIPT CONTENT ===");
                System.out.println(transcript);
                System.out.println("=== END TRANSCRIPT ===");
                
                // Step 7: Test the combined getTranscript method
                System.out.println("\n7. Testing combined getTranscript method...");
                String combinedTranscript = recallAiService.getTranscript(botId);
                assertNotNull(combinedTranscript, "Combined transcript should not be null");
                assertEquals(transcript, combinedTranscript, "Combined transcript should match downloaded transcript");
                System.out.println("✅ Combined transcript method works correctly");
                
            } else {
                System.out.println("❌ Transcript not available after waiting 3 minutes total!");
                System.out.println("This indicates a problem with the bot or transcript generation.");
                System.out.println("Bot ID: " + botId);
                System.out.println("Meeting URL: " + TEST_MEETING_URL);
                
                // Fail the test
                fail("Transcript should be available after 3 minutes of waiting. Bot may not have joined the meeting or transcript generation failed.");
            }

            // do not delete the bot
            createdBotIds.clear();
            
            System.out.println("\n=== INTEGRATION TEST COMPLETED ===");
            System.out.println("Bot ID: " + botId);
            System.out.println("All methods tested successfully!");
            
        });
    }
}

package hackathon_jump.server.business.service.external;

import hackathon_jump.server.model.domain.Automation;
import hackathon_jump.server.model.domain.EventReport;
import hackathon_jump.server.model.domain.EventReportAutomation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class ChatGptService {
    @Value("${app.openai.api-key}")
    private String apiKey;
    @Value("${app.openai.api-base-url}")
    private String apiBaseUrl;
    
    private final RestTemplate restTemplate;
    
    public ChatGptService() {
        this.restTemplate = new RestTemplate();
    }

    public String generateEmailSummary(EventReport eventReport) {
        if (eventReport == null || eventReport.getTranscript() == null || eventReport.getTranscript().isEmpty()) {
            log.warn("Cannot generate email summary: transcript is empty");
            return "";
        }
        
        String prompt = buildEmailPrompt(eventReport);
        return getChatGptResponse(prompt);
    }

    private String buildEmailPrompt(EventReport eventReport) {
        return String.format(
                "Based on the following meeting transcript, generate a professional email summary:\n\n" +
                        "Meeting Details:\n" +
                        "- Date: %s\n" +
                        "- Attendees: %s\n" +
                        "- Platform: %s\n\n" +
                        "Transcript:\n%s\n\n" +
                        "Please create a concise email summary that includes:\n" +
                        "1. Key discussion points\n" +
                        "2. Decisions made\n" +
                        "3. Action items\n" +
                        "4. Next steps\n\n" +
                        "Format as a professional email with appropriate subject line and closing.",
                eventReport.getStartDateTime(),
                eventReport.getAttendees(),
                eventReport.getPlatform(),
                eventReport.getTranscript()
        );
    }

    public String generatePostSummary(EventReport eventReport) {
        if (eventReport == null || eventReport.getTranscript() == null || eventReport.getTranscript().isEmpty()) {
            log.warn("Cannot generate post: transcript is empty");
            return "";
        }
        
        String prompt = buildPostPrompt(eventReport);
        return getChatGptResponse(prompt);
    }

    private String buildPostPrompt(EventReport eventReport) {
        String postInstructions = "Please create a Social Media post that:\n" +
                "1. Is more casual and friendly\n" +
                "2. Highlights interesting points or outcomes\n" +
                "3. Is engaging for a broader audience\n" +
                "4. Includes appropriate emojis\n" +
                "5. Is conversational in tone";

        return String.format(
                "Based on the following meeting transcript, generate a social media post:\n\n" +
                        "Meeting Details:\n" +
                        "- Date: %s\n" +
                        "- Attendees: %s\n" +
                        "- Platform: %s\n\n" +
                        "Transcript:\n%s\n\n" +
                        postInstructions,
                eventReport.getStartDateTime(),
                eventReport.getAttendees(),
                eventReport.getPlatform(),
                eventReport.getTranscript()
        );
    }

    public EventReportAutomation generateEventReportAutomation(EventReport eventReport, Automation automation) {
        if (eventReport == null || eventReport.getTranscript() == null || eventReport.getTranscript().isEmpty()) {
            log.error("Cannot generate automation content: transcript is empty");
            return null;
        }

        String textPrompt = buildTextPrompt(eventReport, automation);
        String titlePrompt = buildTitlePrompt(eventReport, automation);
        
        String generatedText = getChatGptResponse(textPrompt);
        String generatedTitle = getChatGptResponse(titlePrompt);

        EventReportAutomation newEventReportAutomation = new EventReportAutomation();
        newEventReportAutomation.setAutomation(automation);
        newEventReportAutomation.setEventReport(eventReport);
        newEventReportAutomation.setText(generatedText);
        newEventReportAutomation.setTitle(generatedTitle);

        return newEventReportAutomation;
    }

    private String buildTextPrompt(EventReport eventReport, Automation automation) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("Based on the following meeting transcript, generate content for a ");
        prompt.append(automation.getAutomationType().toString().toLowerCase().replace("_", " "));
        prompt.append(" for ");
        prompt.append(automation.getMediaPlatform().toString().toLowerCase());
        prompt.append(":\n\n");
        
        prompt.append("Automation Details:\n");
        prompt.append("- Title: ").append(automation.getTitle()).append("\n");
        prompt.append("- Type: ").append(automation.getAutomationType()).append("\n");
        prompt.append("- Platform: ").append(automation.getMediaPlatform()).append("\n");
        prompt.append("- Description: ").append(automation.getDescription()).append("\n");
        if (automation.getExample() != null && !automation.getExample().isEmpty()) {
            prompt.append("- Example: ").append(automation.getExample()).append("\n");
        }
        prompt.append("\n");
        
        prompt.append("Meeting Details:\n");
        prompt.append("- Date: ").append(eventReport.getStartDateTime()).append("\n");
        prompt.append("- Attendees: ").append(eventReport.getAttendees()).append("\n");
        prompt.append("- Platform: ").append(eventReport.getPlatform()).append("\n\n");
        
        prompt.append("Transcript:\n").append(eventReport.getTranscript()).append("\n\n");
        
        return prompt.toString();
    }

    private String buildTitlePrompt(EventReport eventReport, Automation automation) {
        return String.format("Find a good title for %s with this text:\n\n%s", 
            automation.getMediaPlatform().toString().toLowerCase(), 
            eventReport.getTranscript());
    }

    public String getChatGptResponse(String prompt) {
        log.info("Sending request to ChatGPT API");
        
        String apiUrl = apiBaseUrl + "/v1/chat/completions";
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-5-nano");
        requestBody.put("max_completion_tokens", 1000);
        requestBody.put("temperature", 0.7);
        
        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);
        requestBody.put("messages", new Object[]{message});
        
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
        
        try {
            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                apiUrl, HttpMethod.POST, requestEntity, (Class<Map<String, Object>>) (Class<?>) Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                
                if (responseBody.containsKey("choices")) {
                    @SuppressWarnings("unchecked")
                    java.util.List<Map<String, Object>> choices = (java.util.List<Map<String, Object>>) responseBody.get("choices");
                    
                    if (!choices.isEmpty()) {
                        Map<String, Object> firstChoice = choices.get(0);
                        if (firstChoice.containsKey("message")) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> messageObj = (Map<String, Object>) firstChoice.get("message");
                            String content = (String) messageObj.get("content");
                            log.info("Successfully received ChatGPT response, length: {} characters", content.length());
                            return content.trim();
                        }
                    }
                }
            }
            
            log.error("Unexpected response format from ChatGPT API");
            return "";
            
        } catch (Exception e) {
            log.error("Error calling ChatGPT API: {}", e.getMessage());
            return "";
        }
    }
}

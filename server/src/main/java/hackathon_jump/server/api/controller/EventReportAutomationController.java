package hackathon_jump.server.api.controller;

import hackathon_jump.server.business.service.calendar.EventReportAutomationService;
import hackathon_jump.server.business.service.external.LinkedinService;
import hackathon_jump.server.infrastructure.repository.IAutomationRepository;
import hackathon_jump.server.infrastructure.repository.IEventReportRepository;
import hackathon_jump.server.infrastructure.repository.IUserRepository;
import hackathon_jump.server.model.domain.Automation;
import hackathon_jump.server.model.domain.EventReport;
import hackathon_jump.server.model.domain.EventReportAutomation;
import hackathon_jump.server.model.domain.User;
import hackathon_jump.server.model.dto.Session;
import hackathon_jump.server.model.enums.EOauthProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/event-report-automations")
@Slf4j
public class EventReportAutomationController {
    
    @Autowired
    private EventReportAutomationService eventReportAutomationService;
    
    @Autowired
    private IAutomationRepository automationRepository;
    
    @Autowired
    private IEventReportRepository eventReportRepository;
    
    @Autowired
    private IUserRepository userRepository;
    
    @Autowired
    private LinkedinService linkedinService;
    
    /**
     * Get EventReportAutomation by automation and event report IDs.
     * Creates a new one using ChatGPT if not present.
     */
    @GetMapping("/automation/{automationId}/event-report/{eventReportId}")
    public ResponseEntity<EventReportAutomation> getByAutomationAndEventReport(
            @PathVariable Long automationId,
            @PathVariable Long eventReportId,
            @RequestAttribute("session") Session session) {
        
        log.info("Getting EventReportAutomation for automation ID: {} and event report ID: {}", 
                automationId, eventReportId);
        
        try {
            // Verify user has access to the automation
            if (!hasAccessToAutomation(automationId, session)) {
                log.warn("User does not have access to automation ID: {}", automationId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            Optional<Automation> automationOpt = automationRepository.findById(automationId);
            if (automationOpt.isEmpty()) {
                log.error("Automation not found with ID: {}", automationId);
                return ResponseEntity.notFound().build();
            }
            
            Optional<EventReport> eventReportOpt = eventReportRepository.findById(eventReportId);
            if (eventReportOpt.isEmpty()) {
                log.error("EventReport not found with ID: {}", eventReportId);
                return ResponseEntity.notFound().build();
            }
            
            Automation automation = automationOpt.get();
            EventReport eventReport = eventReportOpt.get();
            
            EventReportAutomation result = eventReportAutomationService
                    .getByAutomationAndEventReport(automation, eventReport);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error getting EventReportAutomation: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get all EventReportAutomation by automation ID
     */
    @GetMapping("/automation/{automationId}")
    public ResponseEntity<List<EventReportAutomation>> getByAutomation(
            @PathVariable Long automationId,
            @RequestAttribute("session") Session session) {
        
        log.info("Getting all EventReportAutomation for automation ID: {}", automationId);
        
        try {
            if (!hasAccessToAutomation(automationId, session)) {
                log.warn("User does not have access to automation ID: {}", automationId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            Optional<Automation> automationOpt = automationRepository.findById(automationId);
            if (automationOpt.isEmpty()) {
                log.error("Automation not found with ID: {}", automationId);
                return ResponseEntity.notFound().build();
            }
            
            List<EventReportAutomation> result = eventReportAutomationService
                    .getByAutomation(automationOpt.get());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error getting EventReportAutomation by automation: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get all EventReportAutomation by event report ID
     */
    @GetMapping("/event-report/{eventReportId}")
    public ResponseEntity<List<EventReportAutomation>> getByEventReport(
            @PathVariable Long eventReportId,
            @RequestAttribute("session") Session session) {
        
        log.info("Getting all EventReportAutomation for event report ID: {}", eventReportId);
        
        try {
            Optional<EventReport> eventReportOpt = eventReportRepository.findById(eventReportId);
            if (eventReportOpt.isEmpty()) {
                log.error("EventReport not found with ID: {}", eventReportId);
                return ResponseEntity.notFound().build();
            }
            
            List<EventReportAutomation> result = eventReportAutomationService
                    .getByEventReport(eventReportOpt.get());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error getting EventReportAutomation by event report: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Refresh EventReportAutomation by calling ChatGPT again
     */
    @PutMapping("/{id}/refresh")
    public ResponseEntity<EventReportAutomation> refresh(
            @PathVariable Long id,
            @RequestAttribute("session") Session session) {
        
        log.info("Refreshing EventReportAutomation with ID: {}", id);
        
        try {
            EventReportAutomation refreshed = eventReportAutomationService.refresh(id);
            return ResponseEntity.ok(refreshed);
            
        } catch (RuntimeException e) {
            log.error("Error refreshing EventReportAutomation: {}", e.getMessage());
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            log.error("Unexpected error refreshing EventReportAutomation: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Post EventReportAutomation to LinkedIn
     */
    @PostMapping("/{id}/linkedin")
    public ResponseEntity<Map<String, String>> postToLinkedin(
            @PathVariable Long id,
            @RequestAttribute("session") Session session) {
        
        log.info("Posting EventReportAutomation with ID: {} to LinkedIn", id);
        
        try {
            Optional<EventReportAutomation> eventReportAutomationOpt = eventReportAutomationService.getById(id);
            if (eventReportAutomationOpt.isEmpty()) {
                log.error("EventReportAutomation not found with ID: {}", id);
                return ResponseEntity.notFound().build();
            }
            
            EventReportAutomation eventReportAutomation = eventReportAutomationOpt.get();
            if (!hasAccessToAutomation(eventReportAutomation.getAutomation().getId(), session)) {
                log.warn("User does not have access to automation ID: {}", eventReportAutomation.getAutomation().getId());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            String title = eventReportAutomation.getTitle() != null ? eventReportAutomation.getTitle() : "Meeting Summary";
            String text = eventReportAutomation.getText() != null ? eventReportAutomation.getText() : "";

            String linkedinUsername = session.getLinkedinUsername();
            if (linkedinUsername == null) {
                log.error("No user email found in session for LinkedIn posting");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            log.debug("Post for user {}", linkedinUsername);
            linkedinService.post(title, text, getLinkedInAccessToken(linkedinUsername));
            log.info("Successfully posted EventReportAutomation {} to LinkedIn with title: '{}'", id, title);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Successfully posted to LinkedIn");
            response.put("title", title);
            response.put("textLength", String.valueOf(text.length()));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error posting EventReportAutomation {} to LinkedIn: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String getLinkedInAccessToken(String linkedinUsername) {
        Optional<User> userOpt = userRepository.findByUsernameAndProvider(linkedinUsername, EOauthProvider.LINKEDIN);
        return userOpt.map(User::getOauthToken).orElse(null);
    }
    
    /**
     * Delete EventReportAutomation by ID
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @RequestAttribute("session") Session session) {
        
        log.info("Deleting EventReportAutomation with ID: {}", id);
        
        try {
            eventReportAutomationService.delete(id);
            return ResponseEntity.noContent().build();
            
        } catch (RuntimeException e) {
            log.error("Error deleting EventReportAutomation: {}", e.getMessage());
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            log.error("Unexpected error deleting EventReportAutomation: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Check if user has access to the automation
     */
    private boolean hasAccessToAutomation(Long automationId, Session session) {
        try {
            List<User> users = getUsersFromSession(session);
            for (User user : users) {
                Optional<Automation> automationOpt = automationRepository.findByIdAndUserId(automationId, user.getId());
                if (automationOpt.isPresent()) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            log.error("Error checking automation access: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Get users from session (same pattern as AutomationService)
     */
    private List<User> getUsersFromSession(Session session) {
        return session.getGoogleEmailAddresses().stream()
                .map(email -> userRepository.findByUsernameAndProvider(email, EOauthProvider.GOOGLE))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }
    
    /**
     * Get the first user email from session for LinkedIn posting
     */
    private String getFirstUserEmail(Session session) {
        if (session.getGoogleEmailAddresses().isEmpty()) {
            return null;
        }
        return session.getGoogleEmailAddresses().get(0);
    }
}

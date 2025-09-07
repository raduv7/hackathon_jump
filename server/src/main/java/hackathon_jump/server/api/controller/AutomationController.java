package hackathon_jump.server.api.controller;

import hackathon_jump.server.business.service.auth.AutomationService;
import hackathon_jump.server.model.domain.Automation;
import hackathon_jump.server.model.dto.Session;
import hackathon_jump.server.model.enums.EAutomationType;
import hackathon_jump.server.model.enums.EMediaPlatform;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/automations")
@Slf4j
public class AutomationController {
    
    @Autowired
    private AutomationService automationService;
    
    /**
     * Get all automations for the current user (from session)
     * Returns only title, type, and platform information
     */
    @GetMapping({"", "/"})
    public ResponseEntity<List<Automation>> getAllUserAutomations(@RequestAttribute("session") Session session) {
        try {
            if (session.getGoogleEmailAddresses().isEmpty()) {
                log.warn("No Google email addresses found in session");
                return ResponseEntity.badRequest().build();
            }
            
            List<Automation> automations = automationService.getAllUserAutomations(session);
            log.info("Retrieved {} automations for user session", automations.size());
            return ResponseEntity.ok(automations);
        } catch (Exception e) {
            log.error("Error getting user automations: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get a specific automation by ID for the current user
     */
    @GetMapping("/{id}")
    public ResponseEntity<Automation> getAutomationById(@RequestAttribute("session") Session session,
                                                       @PathVariable Long id) {
        try {
            if (session.getGoogleEmailAddresses().isEmpty()) {
                log.warn("No Google email addresses found in session");
                return ResponseEntity.badRequest().build();
            }
            
            Optional<Automation> automation = automationService.getAutomationById(session, id);
            if (automation.isPresent()) {
                log.info("Retrieved automation {} for user session", id);
                return ResponseEntity.ok(automation.get());
            } else {
                log.warn("Automation {} not found or user doesn't have access", id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error getting automation {}: {}", id, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Create a new automation and associate it with the current user
     */
    @PostMapping
    public ResponseEntity<Automation> createAutomation(@RequestAttribute("session") Session session,
                                                      @RequestParam String title,
                                                      @RequestParam EAutomationType automationType,
                                                      @RequestParam EMediaPlatform mediaPlatform,
                                                      @RequestParam String description,
                                                      @RequestParam(required = false) String example) {
        try {
            if (session.getGoogleEmailAddresses().isEmpty()) {
                log.warn("No Google email addresses found in session");
                return ResponseEntity.badRequest().build();
            }
            
            Automation automation = automationService.createAutomation(session, title, automationType, 
                mediaPlatform, description, example);
            log.info("Created automation {} for user session", automation.getId());
            return ResponseEntity.ok(automation);
        } catch (Exception e) {
            log.error("Error creating automation: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Update an existing automation (only if user has access)
     */
    @PutMapping("/{id}")
    public ResponseEntity<Automation> updateAutomation(@RequestAttribute("session") Session session,
                                                      @PathVariable Long id,
                                                      @RequestParam String title,
                                                      @RequestParam EAutomationType automationType,
                                                      @RequestParam EMediaPlatform mediaPlatform,
                                                      @RequestParam String description,
                                                      @RequestParam(required = false) String example) {
        try {
            if (session.getGoogleEmailAddresses().isEmpty()) {
                log.warn("No Google email addresses found in session");
                return ResponseEntity.badRequest().build();
            }
            
            Optional<Automation> automation = automationService.updateAutomation(session, id, title,
                automationType, mediaPlatform, description, example);
            
            if (automation.isPresent()) {
                log.info("Updated automation {} for user session", id);
                return ResponseEntity.ok(automation.get());
            } else {
                log.warn("Automation {} not found or user doesn't have access", id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error updating automation {}: {}", id, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Delete an automation (only if user has access)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteAutomation(@RequestAttribute("session") Session session,
                                                  @PathVariable Long id) {
        try {
            if (session.getGoogleEmailAddresses().isEmpty()) {
                log.warn("No Google email addresses found in session");
                return ResponseEntity.badRequest().build();
            }
            
            boolean deleted = automationService.deleteAutomation(session, id);
            if (deleted) {
                log.info("Deleted automation {} for user session", id);
                return ResponseEntity.ok("Automation deleted successfully");
            } else {
                log.warn("Automation {} not found or user doesn't have access", id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error deleting automation {}: {}", id, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Add an automation to the current user's collection
     */
    @PostMapping("/{id}/subscribe")
    public ResponseEntity<String> subscribeToAutomation(@RequestAttribute("session") Session session,
                                                       @PathVariable Long id) {
        try {
            if (session.getGoogleEmailAddresses().isEmpty()) {
                log.warn("No Google email addresses found in session");
                return ResponseEntity.badRequest().build();
            }
            
            boolean added = automationService.addAutomationToUser(session, id);
            if (added) {
                log.info("User subscribed to automation {}", id);
                return ResponseEntity.ok("Successfully subscribed to automation");
            } else {
                log.warn("Automation {} not found", id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error subscribing to automation {}: {}", id, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Remove an automation from the current user's collection
     */
    @DeleteMapping("/{id}/subscribe")
    public ResponseEntity<String> unsubscribeFromAutomation(@RequestAttribute("session") Session session,
                                                           @PathVariable Long id) {
        try {
            if (session.getGoogleEmailAddresses().isEmpty()) {
                log.warn("No Google email addresses found in session");
                return ResponseEntity.badRequest().build();
            }
            
            boolean removed = automationService.removeAutomationFromUser(session, id);
            if (removed) {
                log.info("User unsubscribed from automation {}", id);
                return ResponseEntity.ok("Successfully unsubscribed from automation");
            } else {
                log.warn("Automation {} not found", id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error unsubscribing from automation {}: {}", id, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}

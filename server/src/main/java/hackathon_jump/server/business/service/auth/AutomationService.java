package hackathon_jump.server.business.service.auth;

import hackathon_jump.server.business.service.calendar.EventReportAutomationService;
import hackathon_jump.server.infrastructure.repository.IAutomationRepository;
import hackathon_jump.server.infrastructure.repository.IUserRepository;
import hackathon_jump.server.model.domain.Automation;
import hackathon_jump.server.model.domain.User;
import hackathon_jump.server.model.dto.Session;
import hackathon_jump.server.model.enums.EAutomationType;
import hackathon_jump.server.model.enums.EMediaPlatform;
import hackathon_jump.server.model.enums.EOauthProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class AutomationService {
    
    @Autowired
    private IAutomationRepository automationRepository;
    
    @Autowired
    private IUserRepository userRepository;
    
    @Autowired
    private EventReportAutomationService eventReportAutomationService;
    
    /**
     * Get all automations for the current user (from session)
     * Returns only title, type, and platform information
     */
    public List<Automation> getAllUserAutomations(Session session) {
        List<User> users = getUsersFromSession(session);
        if (users.isEmpty()) {
            return List.of();
        }

        return new ArrayList<>(users.getFirst().getAutomations());
    }
    
    /**
     * Get a specific automation by ID for the current user
     */
    public Optional<Automation> getAutomationById(Session session, Long automationId) {
        List<User> users = getUsersFromSession(session);
        if (users.isEmpty()) {
            return Optional.empty();
        }
        
        // Check if any user in the session has access to this automation
        for (User user : users) {
            Optional<Automation> automation = automationRepository.findByIdAndUserId(automationId, user.getId());
            if (automation.isPresent()) {
                return automation;
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * Create a new automation and associate it with the current user
     * @param title The title of the automation
     */
    @Transactional
    public Automation createAutomation(Session session, String title, EAutomationType automationType, 
                                     EMediaPlatform mediaPlatform, String description, String example) {
        Automation automation = new Automation();
        automation.setTitle(title);
        automation.setAutomationType(automationType);
        automation.setMediaPlatform(mediaPlatform);
        automation.setDescription(description);
        automation.setExample(example);

        // Save the automation first
        Automation savedAutomation = automationRepository.save(automation);
        
        // Then associate with users (only manage from the owning side - User)
        List<User> users = getUsersFromSession(session);
        for (User user : users) {
            user.getAutomations().add(savedAutomation);
        }
        
        // Save users without flush to avoid transaction conflicts
        userRepository.saveAll(users);
        
        log.info("Created automation {} and associated with {} users", savedAutomation.getId(), users.size());
        return savedAutomation;
    }
    
    /**
     * Update an existing automation (only if user has access)
     * @param title The title of the automation
     */
    @Transactional
    public Optional<Automation> updateAutomation(Session session, Long automationId, String title,
                                               EAutomationType automationType, EMediaPlatform mediaPlatform,
                                               String description, String example) {
        Optional<Automation> automationOpt = getAutomationById(session, automationId);
        
        if (automationOpt.isPresent()) {
            Automation automation = automationOpt.get();
            automation.setTitle(title);
            automation.setAutomationType(automationType);
            automation.setMediaPlatform(mediaPlatform);
            automation.setDescription(description);
            automation.setExample(example);
            
            Automation updatedAutomation = automationRepository.save(automation);
            log.info("Updated automation {}", automationId);
            return Optional.of(updatedAutomation);
        }
        
        return Optional.empty();
    }
    
    /**
     * Delete an automation (only if user has access)
     */
    @Transactional
    public boolean deleteAutomation(Session session, Long automationId) {
        Optional<Automation> automationOpt = getAutomationById(session, automationId);
        
        if (automationOpt.isPresent()) {
            Automation automation = automationOpt.get();
            
            // Delete all associated EventReportAutomation first
            eventReportAutomationService.deleteByAutomationId(automationId);
            log.info("Deleted all EventReportAutomation for automation {}", automationId);
            
            // Remove from all users
            List<User> users = getUsersFromSession(session);
            for (User user : users) {
                user.getAutomations().remove(automation);
            }
            // Save all users at once to avoid multiple individual saves
            userRepository.saveAll(users);
            
            // Then delete the automation
            automationRepository.delete(automation);
            log.info("Deleted automation {}", automationId);
            return true;
        }
        
        return false;
    }
    
    /**
     * Add an automation to the current user's collection
     */
    @Transactional
    public boolean addAutomationToUser(Session session, Long automationId) {
        Optional<Automation> automationOpt = automationRepository.findById(automationId);
        if (automationOpt.isEmpty()) {
            return false;
        }
        
        List<User> users = getUsersFromSession(session);
        Automation automation = automationOpt.get();
        
        for (User user : users) {
            user.getAutomations().add(automation);
        }
        
        // Save all users at once to avoid multiple individual saves
        userRepository.saveAll(users);
        
        log.info("Added automation {} to {} users", automationId, users.size());
        return true;
    }
    
    /**
     * Remove an automation from the current user's collection
     */
    @Transactional
    public boolean removeAutomationFromUser(Session session, Long automationId) {
        List<User> users = getUsersFromSession(session);
        Optional<Automation> automationOpt = automationRepository.findById(automationId);
        
        if (automationOpt.isEmpty()) {
            return false;
        }
        
        Automation automation = automationOpt.get();
        for (User user : users) {
            user.getAutomations().remove(automation);
        }
        
        // Save all users at once to avoid multiple individual saves
        userRepository.saveAll(users);
        
        log.info("Removed automation {} from {} users", automationId, users.size());
        return true;
    }
    
    /**
     * Helper method to get users from session based on Google email addresses
     */
    private List<User> getUsersFromSession(Session session) {
        return session.getGoogleEmailAddresses().stream()
                .map(email -> userRepository.findByUsernameAndProvider(email, EOauthProvider.GOOGLE))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }
}

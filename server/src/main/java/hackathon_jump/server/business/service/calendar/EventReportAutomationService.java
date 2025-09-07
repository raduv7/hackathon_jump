package hackathon_jump.server.business.service.calendar;

import hackathon_jump.server.business.service.external.ChatGptService;
import hackathon_jump.server.infrastructure.repository.IEventReportAutomationRepository;
import hackathon_jump.server.model.domain.Automation;
import hackathon_jump.server.model.domain.EventReport;
import hackathon_jump.server.model.domain.EventReportAutomation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class EventReportAutomationService {
    
    @Autowired
    private IEventReportAutomationRepository eventReportAutomationRepository;
    
    @Autowired
    private ChatGptService chatGptService;
    
    /**
     * Get EventReportAutomation by automation and event report.
     * If not present, creates a new one using ChatGPT service.
     */
    public EventReportAutomation getByAutomationAndEventReport(Automation automation, EventReport eventReport) {
        log.info("Getting EventReportAutomation for automation ID: {} and event report ID: {}", 
                automation.getId(), eventReport.getId());
        
        // First, check if it exists in a separate transaction
        Optional<EventReportAutomation> existing = findExistingEventReportAutomation(automation, eventReport);
        
        if (existing.isPresent()) {
            log.info("Found existing EventReportAutomation with ID: {}", existing.get().getId());
            return existing.get();
        }
        
        log.info("No existing EventReportAutomation found, creating new one with ChatGPT");
        return createNewEventReportAutomation(automation, eventReport);
    }
    
    /**
     * Find existing EventReportAutomation in a separate, short transaction
     */
    @Transactional(readOnly = true)
    public Optional<EventReportAutomation> findExistingEventReportAutomation(Automation automation, EventReport eventReport) {
        return eventReportAutomationRepository.findByAutomationAndEventReport(automation, eventReport);
    }

    private EventReportAutomation createNewEventReportAutomation(Automation automation, EventReport eventReport) {
        // Generate content outside of transaction to avoid long-running database locks
        EventReportAutomation newEventReportAutomation = chatGptService.generateEventReportAutomation(eventReport, automation);
        
        // Save in a separate, short transaction
        return saveEventReportAutomation(newEventReportAutomation);
    }
    
    /**
     * Save EventReportAutomation in a separate, short transaction
     */
    @Transactional
    public EventReportAutomation saveEventReportAutomation(EventReportAutomation eventReportAutomation) {
        EventReportAutomation saved = eventReportAutomationRepository.save(eventReportAutomation);
        log.info("Created new EventReportAutomation with ID: {}, title: '{}', text length: {}", 
                saved.getId(), eventReportAutomation.getTitle(), 
                eventReportAutomation.getText() != null ? eventReportAutomation.getText().length() : 0);
        
        return saved;
    }

    public EventReportAutomation refresh(Long id) {
        log.info("Refreshing EventReportAutomation with ID: {}", id);
        
        // Get existing record in a separate transaction
        EventReportAutomation existing = getEventReportAutomationById(id);
        Automation automation = existing.getAutomation();
        EventReport eventReport = existing.getEventReport();

        // Generate new content outside of transaction
        EventReportAutomation newEventReportAutomation = chatGptService.generateEventReportAutomation(eventReport, automation);
        
        // Update the existing record with new content
        existing.setText(newEventReportAutomation.getText());
        existing.setTitle(newEventReportAutomation.getTitle());
        
        // Save in a separate, short transaction
        return updateEventReportAutomation(existing);
    }
    
    /**
     * Get EventReportAutomation by ID in a separate transaction
     */
    @Transactional(readOnly = true)
    public EventReportAutomation getEventReportAutomationById(Long id) {
        Optional<EventReportAutomation> existing = eventReportAutomationRepository.findById(id);
        if (existing.isEmpty()) {
            log.error("EventReportAutomation not found with ID: {}", id);
            throw new RuntimeException("EventReportAutomation not found with ID: " + id);
        }
        return existing.get();
    }
    
    /**
     * Update EventReportAutomation in a separate, short transaction
     */
    @Transactional
    public EventReportAutomation updateEventReportAutomation(EventReportAutomation eventReportAutomation) {
        EventReportAutomation updated = eventReportAutomationRepository.save(eventReportAutomation);
        log.info("Refreshed EventReportAutomation with ID: {}, new title: '{}', new text length: {}", 
                updated.getId(), updated.getTitle(), 
                updated.getText() != null ? updated.getText().length() : 0);
        
        return updated;
    }

    @Transactional
    public void delete(Long id) {
        log.info("Deleting EventReportAutomation with ID: {}", id);
        
        if (!eventReportAutomationRepository.existsById(id)) {
            log.error("EventReportAutomation not found with ID: {}", id);
            throw new RuntimeException("EventReportAutomation not found with ID: " + id);
        }
        
        eventReportAutomationRepository.deleteById(id);
        log.info("Deleted EventReportAutomation with ID: {}", id);
    }

    @Transactional
    public void deleteByAutomationId(Long automationId) {
        log.info("Deleting all EventReportAutomation for automation ID: {}", automationId);
        
        List<EventReportAutomation> toDelete = eventReportAutomationRepository.findByAutomation_Id(automationId);
        int count = toDelete.size();
        
        eventReportAutomationRepository.deleteByAutomation_Id(automationId);
        log.info("Deleted {} EventReportAutomation records for automation ID: {}", count, automationId);
    }

    @Transactional
    public void deleteByEventReportId(Long eventReportId) {
        log.info("Deleting all EventReportAutomation for event report ID: {}", eventReportId);
        
        List<EventReportAutomation> toDelete = eventReportAutomationRepository.findByEventReport_Id(eventReportId);
        int count = toDelete.size();
        
        eventReportAutomationRepository.deleteByEventReport_Id(eventReportId);
        log.info("Deleted {} EventReportAutomation records for event report ID: {}", count, eventReportId);
    }

    @Transactional(readOnly = true)
    public List<EventReportAutomation> getByAutomation(Automation automation) {
        log.info("Getting all EventReportAutomation for automation ID: {}", automation.getId());
        return eventReportAutomationRepository.findByAutomation(automation);
    }
    
    /**
     * Get all EventReportAutomation by event report
     */
    @Transactional(readOnly = true)
    public List<EventReportAutomation> getByEventReport(EventReport eventReport) {
        log.info("Getting all EventReportAutomation for event report ID: {}", eventReport.getId());
        return eventReportAutomationRepository.findByEventReport(eventReport);
    }
    
    /**
     * Get EventReportAutomation by ID
     */
    @Transactional(readOnly = true)
    public Optional<EventReportAutomation> getById(Long id) {
        log.info("Getting EventReportAutomation with ID: {}", id);
        return eventReportAutomationRepository.findById(id);
    }
}
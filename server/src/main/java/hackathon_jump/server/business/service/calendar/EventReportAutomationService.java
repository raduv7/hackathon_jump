package hackathon_jump.server.business.service.calendar;

import hackathon_jump.server.business.service.external.ChatGptService;
import hackathon_jump.server.infrastructure.repository.IEventReportAutomationRepository;
import hackathon_jump.server.model.domain.Automation;
import hackathon_jump.server.model.domain.EventReport;
import hackathon_jump.server.model.domain.EventReportAutomation;
import hackathon_jump.server.model.enums.EMediaPlatform;
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
    @Transactional
    public EventReportAutomation getByAutomationAndEventReport(Automation automation, EventReport eventReport) {
        log.info("Getting EventReportAutomation for automation ID: {} and event report ID: {}", 
                automation.getId(), eventReport.getId());
        
        Optional<EventReportAutomation> existing = eventReportAutomationRepository
                .findByAutomationAndEventReport(automation, eventReport);
        
        if (existing.isPresent()) {
            log.info("Found existing EventReportAutomation with ID: {}", existing.get().getId());
            return existing.get();
        }
        
        log.info("No existing EventReportAutomation found, creating new one with ChatGPT");
        return createNewEventReportAutomation(automation, eventReport);
    }

    private EventReportAutomation createNewEventReportAutomation(Automation automation, EventReport eventReport) {
        EventReportAutomation newEventReportAutomation = chatGptService.generateEventReportAutomation(eventReport, automation);

        EventReportAutomation saved = eventReportAutomationRepository.save(newEventReportAutomation);
        log.info("Created new EventReportAutomation with ID: {}, title: '{}', text length: {}", 
                saved.getId(), newEventReportAutomation.getTitle(), newEventReportAutomation.getText().length());
        
        return saved;
    }

    private String generateTitleForText(String text) {
        if (text == null || text.trim().isEmpty()) {
            log.warn("Cannot generate title: text is empty");
            return "Untitled";
        }
        
        String prompt = String.format(
            "Based on the following content, generate a concise and engaging title (maximum 10 words):\n\n%s\n\n" +
            "Please provide only the title, no additional text or formatting.",
            text
        );
        
        String title = chatGptService.getChatGptResponse(prompt);
        if (title == null || title.trim().isEmpty()) {
            log.warn("ChatGPT returned empty title, using fallback");
            return "Generated Content";
        }
        
        // Clean up the title (remove quotes, extra whitespace, etc.)
        title = title.trim().replaceAll("^[\"']|[\"']$", "").trim();
        
        log.info("Generated title: '{}'", title);
        return title;
    }
    
    /**
     * Generate generic text when platform is not recognized
     */
    private String generateGenericText(EventReport eventReport) {
        return String.format(
            "Meeting Summary for %s\n\n" +
            "Date: %s\n" +
            "Attendees: %s\n" +
            "Platform: %s\n\n" +
            "Key Points:\n" +
            "Based on the meeting transcript, here are the main discussion points and outcomes.",
            eventReport.getEvent() != null ? eventReport.getEvent().getTitle() : "Unknown Event",
            eventReport.getStartDateTime(),
            eventReport.getAttendees(),
            eventReport.getPlatform()
        );
    }

    @Transactional
    public EventReportAutomation refresh(Long id) {
        log.info("Refreshing EventReportAutomation with ID: {}", id);
        
        Optional<EventReportAutomation> existing = eventReportAutomationRepository.findById(id);
        if (existing.isEmpty()) {
            log.error("EventReportAutomation not found with ID: {}", id);
            throw new RuntimeException("EventReportAutomation not found with ID: " + id);
        }
        
        EventReportAutomation eventReportAutomation = existing.get();
        Automation automation = eventReportAutomation.getAutomation();
        EventReport eventReport = eventReportAutomation.getEventReport();

        EventReportAutomation newEventReportAutomation = chatGptService.generateEventReportAutomation(eventReport, automation);
        
        eventReportAutomation.setText(newEventReportAutomation.getText());
        eventReportAutomation.setTitle(newEventReportAutomation.getTitle());
        
        EventReportAutomation updated = eventReportAutomationRepository.save(eventReportAutomation);
        log.info("Refreshed EventReportAutomation with ID: {}, new title: '{}', new text length: {}", 
                updated.getId(), newEventReportAutomation.getText(), newEventReportAutomation.getTitle().length());
        
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

    public List<EventReportAutomation> getByAutomation(Automation automation) {
        log.info("Getting all EventReportAutomation for automation ID: {}", automation.getId());
        return eventReportAutomationRepository.findByAutomation(automation);
    }
    
    /**
     * Get all EventReportAutomation by event report
     */
    public List<EventReportAutomation> getByEventReport(EventReport eventReport) {
        log.info("Getting all EventReportAutomation for event report ID: {}", eventReport.getId());
        return eventReportAutomationRepository.findByEventReport(eventReport);
    }
    
    /**
     * Get EventReportAutomation by ID
     */
    public Optional<EventReportAutomation> getById(Long id) {
        log.info("Getting EventReportAutomation with ID: {}", id);
        return eventReportAutomationRepository.findById(id);
    }
}
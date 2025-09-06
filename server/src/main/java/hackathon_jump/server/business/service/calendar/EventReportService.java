package hackathon_jump.server.business.service.calendar;

import hackathon_jump.server.business.service.external.RecallAiService;
import hackathon_jump.server.infrastructure.repository.IEventReportRepository;
import hackathon_jump.server.infrastructure.repository.IUserRepository;
import hackathon_jump.server.model.domain.Event;
import hackathon_jump.server.model.domain.EventReport;
import hackathon_jump.server.model.domain.User;
import hackathon_jump.server.model.dto.Session;
import hackathon_jump.server.model.enums.EOauthProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class EventReportService {
    @Autowired
    private IUserRepository userRepository;
    @Autowired
    private IEventReportRepository eventReportRepository;
    @Autowired
    private RecallAiService recallAiService;

    public List<EventReport> getAll(Session session) {
        List<EventReport> eventReports = new ArrayList<>();

        for(String googleEmailAddress : session.getGoogleEmailAddresses()) {
            User user = this.userRepository.findByUsernameAndProvider(googleEmailAddress, EOauthProvider.GOOGLE).orElseThrow();
            eventReports.addAll(this.eventReportRepository.findAllByEventOwnerAndPlatformIsNotNull(user));
        }

        return eventReports;
    }

    public EventReport createBot(Event event) {
        log.debug("Creating bot for event: {} (ID: {})", event.getTitle(), event.getId());
        
        try {
            String joinAt = event.getStartDateTime().minusMinutes(event.getOwner().getMinutesBeforeMeeting())
                    .atOffset(ZoneOffset.UTC)
                    .format(DateTimeFormatter.ISO_INSTANT);

            String botId = recallAiService.createBot(event.getLink(), joinAt);
            log.debug("Successfully created bot with ID: {} for event: {}", botId, event.getTitle());

            EventReport eventReport = new EventReport();
            eventReport.setBotId(botId);
            EventReport savedEventReport = eventReportRepository.save(eventReport);
            event.setEventReport(eventReport);
            log.debug("Saved EventReport with ID: {} for bot: {}", savedEventReport.getId(), botId);

            return savedEventReport;
        } catch (Exception e) {
            log.error("Failed to create bot for event {}: {}", event.getTitle(), e.getMessage());
            throw new RuntimeException("Failed to create bot for event: " + event.getTitle(), e);
        }
    }

    /**
     * Updates the bot for the given event by creating a new bot and updating the EventReport
     * @param event The event to update the bot for
     * @return The updated EventReport with new bot ID
     */
    public EventReport updateBot(Event event) {
        log.debug("Updating bot for event: {} (ID: {})", event.getTitle(), event.getId());
        
        if (event.getEventReport() == null) {
            log.error("No EventReport found for event: {} (ID: {}). Creating new bot instead.", event.getTitle(), event.getId());
            return null;
        }
        
        String oldBotId = event.getEventReport().getBotId();
        log.debug("Updating bot from {} to new bot for event: {}", oldBotId, event.getTitle());
        
        try {
            String joinAt = event.getStartDateTime().minusMinutes(event.getOwner().getMinutesBeforeMeeting())
                    .atOffset(ZoneOffset.UTC)
                    .format(DateTimeFormatter.ISO_INSTANT);

            String newBotId = recallAiService.createBot(event.getLink(), joinAt);
            log.debug("Successfully created new bot with ID: {} for event: {}", newBotId, event.getTitle());

            EventReport eventReport = event.getEventReport();
            eventReport.setBotId(newBotId);
            EventReport savedEventReport = eventReportRepository.save(eventReport);
            log.debug("Updated EventReport with ID: {} - Old bot: {}, New bot: {}",
                    savedEventReport.getId(), oldBotId, newBotId);

             if (oldBotId != null) {
                 try {
                     recallAiService.deleteScheduledBot(oldBotId);
                     log.debug("Successfully deleted old bot: {}", oldBotId);
                 } catch (Exception e) {
                     log.warn("Failed to delete old bot {}: {}", oldBotId, e.getMessage());
                 }
             }
            
            return savedEventReport;
            
        } catch (Exception e) {
            log.error("Failed to update bot for event {}: {}", event.getTitle(), e.getMessage());
            throw new RuntimeException("Failed to update bot for event: " + event.getTitle(), e);
        }
    }

    /**
     * Deletes the bot associated with the event's EventReport
     * @param event The event containing the EventReport with bot to delete
     */
    public void deleteBot(Event event) {
        if (event.getEventReport() == null || event.getEventReport().getBotId() == null) {
            log.error("No bot to delete for event: {} (ID: {})", event.getTitle(), event.getId());
            return;
        }
        
        String botId = event.getEventReport().getBotId();
        log.debug("Deleting bot: {} for event: {}", botId, event.getTitle());
        
        try {
            EventReport eventReport = eventReportRepository.findByBotId(botId).orElseThrow();
            eventReportRepository.delete(eventReport);

            recallAiService.deleteScheduledBot(botId);
            log.debug("Successfully deleted bot: {} for event: {}", botId, event.getTitle());
        } catch (Exception e) {
            log.error("Failed to delete bot {} for event {}: {}", botId, event.getTitle(), e.getMessage());
            throw new RuntimeException("Failed to delete bot: " + botId, e);
        }
    }
}

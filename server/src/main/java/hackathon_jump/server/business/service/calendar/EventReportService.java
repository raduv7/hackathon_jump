package hackathon_jump.server.business.service.calendar;

import hackathon_jump.server.business.service.external.RecallAiService;
import hackathon_jump.server.infrastructure.repository.IEventReportRepository;
import hackathon_jump.server.infrastructure.repository.IEventRepository;
import hackathon_jump.server.infrastructure.repository.IUserRepository;
import hackathon_jump.server.model.domain.Event;
import hackathon_jump.server.model.domain.EventReport;
import hackathon_jump.server.model.domain.User;
import hackathon_jump.server.model.dto.Session;
import hackathon_jump.server.model.enums.EOauthProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class EventReportService {
    @Autowired
    private IUserRepository userRepository;
    @Autowired
    private IEventRepository eventRepository;
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

        return eventReports
                .stream()
                .sorted(Comparator.comparingInt(eventReport -> eventReport.getStartDateTime().getSecond()))
                .toList();
    }

    public Optional<EventReport> getById(Long id, Session session) {
        log.info("Getting EventReport with ID: {} for session", id);
        
        Optional<EventReport> eventReportOpt = eventReportRepository.findById(id);
        if (eventReportOpt.isEmpty()) {
            log.warn("EventReport not found with ID: {}", id);
            return Optional.empty();
        }
        
        EventReport eventReport = eventReportOpt.get();
        
        // Check if the user has access to this event report
        if (eventReport.getEvent() == null || eventReport.getEvent().getOwner() == null) {
            log.warn("EventReport {} has no associated event or owner", id);
            return Optional.empty();
        }
        
        // Check if any of the session's Google email addresses match the event owner
        boolean hasAccess = session.getGoogleEmailAddresses().stream()
                .anyMatch(email -> {
                    try {
                        User user = userRepository.findByUsernameAndProvider(email, EOauthProvider.GOOGLE).orElse(null);
                        return user != null && user.getId().equals(eventReport.getEvent().getOwner().getId());
                    } catch (Exception e) {
                        log.warn("Error checking user access for email {}: {}", email, e.getMessage());
                        return false;
                    }
                });
        
        if (!hasAccess) {
            log.warn("User does not have access to EventReport with ID: {}", id);
            return Optional.empty();
        }
        
        log.info("Successfully retrieved EventReport with ID: {}", id);
        return Optional.of(eventReport);
    }

    public EventReport createBot(Event event) {
        log.debug("Creating bot for event: {} (ID: {})", event.getTitle(), event.getId());
        
        try {
            // Get the user's timezone (assuming it's stored or default to system timezone)
            ZoneId userTimezone = ZoneId.systemDefault(); // You might want to get this from user preferences
            
            String joinAt = event.getStartDateTime()
                    .minusMinutes(event.getOwner().getMinutesBeforeMeeting())
                    .atZone(userTimezone)
                    .withZoneSameInstant(ZoneOffset.UTC)
                    .format(DateTimeFormatter.ISO_INSTANT);

            String botId = recallAiService.createBot(event.getLink(), joinAt);
            log.debug("Successfully created bot with ID: {} for event: {}", botId, event.getTitle());

            EventReport eventReport = new EventReport();
            eventReport.setBotId(botId);
            EventReport savedEventReport = eventReportRepository.save(eventReport);
            event.setEventReport(eventReport);
            this.eventRepository.save(event);
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
            // Get the user's timezone (assuming it's stored or default to system timezone)
            ZoneId userTimezone = ZoneId.systemDefault(); // You might want to get this from user preferences
            
            String joinAt = event.getStartDateTime()
                    .minusMinutes(event.getOwner().getMinutesBeforeMeeting())
                    .atZone(userTimezone)
                    .withZoneSameInstant(ZoneOffset.UTC)
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

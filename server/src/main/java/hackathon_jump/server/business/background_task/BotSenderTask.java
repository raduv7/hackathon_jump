package hackathon_jump.server.business.background_task;

import hackathon_jump.server.business.service.external.RecallAiService;
import hackathon_jump.server.infrastructure.repository.IEventRepository;
import hackathon_jump.server.model.domain.Event;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class BotSenderTask {
    @Autowired
    private IEventRepository eventRepository;
    @Autowired
    private RecallAiService recallAiService;

    @Scheduled(fixedRate = 60000) // Run every minute (60,000 milliseconds)
    public void execute() {
        log.info("hi Carla");
        
        // Log Recall AI API key status
        this.recallAiService.logApiKeyStatus();

        List<Event> events = this.eventRepository.findAll();
        for(Event event : events) {
            if(event.getStartDateTime().isAfter(LocalDateTime.now()) &&
                !event.isSentBot() &&
                    event.getStartDateTime().minusMinutes(event.getOwner().getMinutesBeforeMeeting()).isBefore(LocalDateTime.now())) {
                sendBot(event);
            }
        }
    }

    private void sendBot(Event event) {
//        this.recallAiService;
        event.setSentBot(true);
        this.eventRepository.save(event);
    }
}

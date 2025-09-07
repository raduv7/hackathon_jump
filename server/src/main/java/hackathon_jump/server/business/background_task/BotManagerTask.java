package hackathon_jump.server.business.background_task;

import hackathon_jump.server.business.service.external.ChatGptService;
import hackathon_jump.server.business.service.external.RecallAiService;
import hackathon_jump.server.infrastructure.repository.IEventReportRepository;
import hackathon_jump.server.infrastructure.repository.IEventRepository;
import hackathon_jump.server.model.domain.Event;
import hackathon_jump.server.model.domain.EventReport;
import hackathon_jump.server.model.enums.EMeetingPlatform;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * this background tasks name is Carla
 */
@Component
@Slf4j
public class BotManagerTask {
    @Autowired
    private IEventReportRepository eventReportRepository;
    @Autowired
    private IEventRepository eventRepository;
    @Autowired
    private RecallAiService recallAiService;
    @Autowired
    private ChatGptService chatGptService;

    @Scheduled(fixedRate = 60000)
    public void execute() {
        int finishedBotsCnt = 0;
        log.info("Carla says hi!");

        this.recallAiService.logApiKeyStatus();

        for(EventReport eventReport : this.eventReportRepository.findAllByPlatformIsNull()) {
            if(this.recallAiService.isTranscriptAvailable(eventReport.getBotId())) {
                finishBot(eventReport);
                ++ finishedBotsCnt;
            }
        }

        log.info("Carla finished {} bots.", finishedBotsCnt);
    }

    private void finishBot(EventReport eventReport) {
        Event event = eventReport.getEvent();
        event.setFinished(true);
        this.eventRepository.save(event);

        eventReport.setPlatform(EMeetingPlatform.fromLink(event.getLink()));
        this.recallAiService.fillEventReport(eventReport);

        eventReport.setEmailText(this.chatGptService.generateEmailSummary(eventReport));
        eventReport.setPostText(this.chatGptService.generatePostSummary(eventReport));

        this.eventReportRepository.save(eventReport);
    }
}

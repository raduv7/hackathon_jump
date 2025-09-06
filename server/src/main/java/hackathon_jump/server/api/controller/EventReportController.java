package hackathon_jump.server.api.controller;

import com.google.api.client.http.HttpStatusCodes;
import hackathon_jump.server.business.service.calendar.EventReportService;
import hackathon_jump.server.business.service.calendar.EventService;
import hackathon_jump.server.model.domain.Event;
import hackathon_jump.server.model.domain.EventReport;
import hackathon_jump.server.model.dto.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/event_reports")
@Slf4j
public class EventReportController {
    @Autowired
    private EventReportService eventReportService;

    @GetMapping({"", "/"})
    public ResponseEntity<List<EventReport>> getEventReports(@RequestAttribute("session") Session session) {
        try {
            List<EventReport> eventReports = eventReportService.getAll(session);
            return ResponseEntity.ok(eventReports);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatusCodes.STATUS_CODE_UNAUTHORIZED).build();
        }
    }
}

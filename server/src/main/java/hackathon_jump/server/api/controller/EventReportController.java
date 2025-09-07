package hackathon_jump.server.api.controller;

import com.google.api.client.http.HttpStatusCodes;
import hackathon_jump.server.business.service.calendar.EventReportService;
import hackathon_jump.server.model.domain.EventReport;
import hackathon_jump.server.model.dto.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

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

    @GetMapping("/{id}")
    public ResponseEntity<EventReport> getEventReport(@PathVariable Long id, @RequestAttribute("session") Session session) {
        try {
            Optional<EventReport> eventReport = eventReportService.getById(id, session);
            if (eventReport.isPresent()) {
                log.info("Successfully retrieved EventReport with ID: {}", id);
                return ResponseEntity.ok(eventReport.get());
            } else {
                log.warn("EventReport not found or access denied for ID: {}", id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error retrieving EventReport with ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatusCodes.STATUS_CODE_UNAUTHORIZED).build();
        }
    }
}

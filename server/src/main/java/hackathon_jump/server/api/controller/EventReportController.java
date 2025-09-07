package hackathon_jump.server.api.controller;

import com.google.api.client.http.HttpStatusCodes;
import hackathon_jump.server.business.service.calendar.EventReportService;
import hackathon_jump.server.business.service.external.LinkedinService;
import hackathon_jump.server.infrastructure.repository.IUserRepository;
import hackathon_jump.server.model.domain.EventReport;
import hackathon_jump.server.model.domain.User;
import hackathon_jump.server.model.dto.Session;
import hackathon_jump.server.model.enums.EOauthProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/event_reports")
@Slf4j
public class EventReportController {
    @Autowired
    private EventReportService eventReportService;
    
    @Autowired
    private LinkedinService linkedinService;
    
    @Autowired
    private IUserRepository userRepository;

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

    @PostMapping("/{id}/linkedin")
    public ResponseEntity<Map<String, String>> postToLinkedin(
            @PathVariable Long id,
            @RequestAttribute("session") Session session) {
        
        log.info("Posting EventReport with ID: {} to LinkedIn", id);
        
        try {
            Optional<EventReport> eventReportOpt = eventReportService.getById(id, session);
            if (eventReportOpt.isEmpty()) {
                log.error("EventReport not found with ID: {}", id);
                return ResponseEntity.notFound().build();
            }
            
            EventReport eventReport = eventReportOpt.get();
            
            String title = eventReport.getEvent() != null && eventReport.getEvent().getTitle() != null 
                ? eventReport.getEvent().getTitle() 
                : "Meeting Summary";
            String text = eventReport.getPostText() != null ? eventReport.getPostText() : "";

            String linkedinUsername = session.getLinkedinUsername();
            if (linkedinUsername == null) {
                log.error("No LinkedIn username found in session for LinkedIn posting");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            log.debug("Post for user {}", linkedinUsername);
            linkedinService.post(title, text, getLinkedInAccessToken(linkedinUsername));
            log.info("Successfully posted EventReport {} to LinkedIn with title: '{}'", id, title);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Successfully posted to LinkedIn");
            response.put("title", title);
            response.put("textLength", String.valueOf(text.length()));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error posting EventReport {} to LinkedIn: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String getLinkedInAccessToken(String linkedinUsername) {
        Optional<User> userOpt = userRepository.findByUsernameAndProvider(linkedinUsername, EOauthProvider.LINKEDIN);
        return userOpt.map(User::getOauthToken).orElse(null);
    }
}

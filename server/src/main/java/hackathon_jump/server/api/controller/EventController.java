package hackathon_jump.server.api.controller;

import hackathon_jump.server.business.service.calendar.EventService;
import hackathon_jump.server.model.dto.Session;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {
    private static final Logger log = LoggerFactory.getLogger(EventController.class);

    @Autowired
    private EventService eventService;

    @GetMapping("/")
    public ResponseEntity<?> getCalendarEvents(@RequestAttribute("session") Session session) {
        try {
//            List<Event> events = googleCalendarService.getCalendarEvents(accessToken);
//            return ResponseEntity.ok(events);
            return ResponseEntity.ok("");
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}

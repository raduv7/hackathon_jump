package hackathon_jump.server.api.controller;

import hackathon_jump.server.business.service.calendar.EventService;
import hackathon_jump.server.model.domain.Event;
import hackathon_jump.server.model.dto.Session;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {
    private static final Logger log = LoggerFactory.getLogger(EventController.class);

    @Autowired
    private EventService eventService;

    @GetMapping("/")
    public ResponseEntity<List<Event>> getEvents(@RequestAttribute("session") Session session) {
        try {
            List<Event> events = eventService.getAll(session);
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{event_id}/should_send_bot/{should_send_bot}")
    public ResponseEntity<String> updateEventShouldSendBot(@RequestAttribute("session") Session session,
                                                           @PathVariable("event_id") Long eventId,
                                                           @PathVariable("should_send_bot") Boolean shouldSendBot) {
        try {
            eventService.setShouldSendBot(session, eventId, shouldSendBot);
            return ResponseEntity.ok("");
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

}

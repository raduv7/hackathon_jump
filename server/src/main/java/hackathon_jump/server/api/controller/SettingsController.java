package hackathon_jump.server.api.controller;

import hackathon_jump.server.business.service.auth.UserService;
import hackathon_jump.server.model.dto.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/settings")
public class SettingsController {
    private static Logger log = LoggerFactory.getLogger(SettingsController.class);

    @Autowired
    private UserService userService;

    @GetMapping("/minutes_before_meeting")
    public ResponseEntity<Integer> getMinutesBeforeMeeting(@RequestAttribute("session") Session session) {
        try {
            return ResponseEntity.ok(userService.getMinutesBeforeMeeting(session));
        }
        catch (Exception e) {
            return new ResponseEntity<>(HttpStatusCode.valueOf(405));
        }
    }

    @PostMapping("/minutes_before_meeting/{minutesBeforeMeeting}")
    public ResponseEntity<String> handleUpdate(@RequestAttribute("session") Session session,
                                                    @PathVariable("minutesBeforeMeeting") Integer minutesBeforeMeeting) {
        try {
            userService.updateMinutesBeforeMeeting(session, minutesBeforeMeeting);

            return ResponseEntity.ok("");
        }
        catch (Exception e) {
            return new ResponseEntity<>(HttpStatusCode.valueOf(405));
        }
    }
}

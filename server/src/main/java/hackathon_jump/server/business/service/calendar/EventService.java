package hackathon_jump.server.business.service.calendar;

import hackathon_jump.server.business.mapper.EventMapper;
import hackathon_jump.server.infrastructure.repository.IUserRepository;
import hackathon_jump.server.model.EOauthProvider;
import hackathon_jump.server.model.domain.Event;
import hackathon_jump.server.model.domain.User;
import hackathon_jump.server.model.dto.Session;
import com.google.api.services.calendar.model.Event as GoogleEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class EventService {
    @Autowired
    private IUserRepository userRepository;
    @Autowired
    private GoogleCalendarService googleCalendarService;
    @Autowired
    private EventMapper eventMapper;

    public List<Event> getAll(Session session) {
        List<Event> allEvents = new ArrayList<>();
        
        for(String googleEmailAddress : session.getGoogleEmailAddresses()) {
            User user = userRepository.findByUsernameAndProvider(googleEmailAddress, EOauthProvider.GOOGLE).orElseThrow();
            List<GoogleEvent> googleEvents = googleCalendarService.getCalendarEvents(user.getOauthToken());
            
            // Map Google events to domain events
            List<Event> mappedEvents = eventMapper.googleEventsToEvents(googleEvents);
            allEvents.addAll(mappedEvents);
            
            log.info("Mapped {} events for user: {}", mappedEvents.size(), googleEmailAddress);
        }
        
        log.info("Total events retrieved: {}", allEvents.size());
        return allEvents;
    }
}

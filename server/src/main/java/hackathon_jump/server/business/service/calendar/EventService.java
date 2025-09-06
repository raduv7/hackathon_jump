package hackathon_jump.server.business.service.calendar;

import hackathon_jump.server.business.mapper.EventMapper;
import hackathon_jump.server.infrastructure.repository.IEventRepository;
import hackathon_jump.server.infrastructure.repository.IUserRepository;
import hackathon_jump.server.model.EOauthProvider;
import hackathon_jump.server.model.domain.Event;
import hackathon_jump.server.model.domain.User;
import hackathon_jump.server.model.dto.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
public class EventService {
    @Autowired
    private IEventRepository eventRepository;
    @Autowired
    private IUserRepository userRepository;
    @Autowired
    private GoogleCalendarService googleCalendarService;
    @Autowired
    private EventMapper eventMapper;

    public List<Event> getAll(Session session) {
        List<Event> events = getAllFromGoogle(session);
        return saveAll(events);
    }

    public void refreshAll(Session session) {
        List<Event> events = getAllFromGoogle(session);
        saveAll(events);
    }

    private List<Event> getAllFromGoogle(Session session) {
        List<Event> allEvents = new ArrayList<>();

        for(String googleEmailAddress : session.getGoogleEmailAddresses()) {
            User user = userRepository.findByUsernameAndProvider(googleEmailAddress, EOauthProvider.GOOGLE).orElseThrow();
            List<com.google.api.services.calendar.model.Event> googleEvents = googleCalendarService.getCalendarEvents(user.getOauthToken());

            // Map Google events to domain events
            List<Event> mappedEvents = eventMapper.googleEventsToEvents(googleEvents, user);
            allEvents.addAll(mappedEvents);

            log.info("Mapped {} events for user: {}", mappedEvents.size(), googleEmailAddress);
        }

        log.info("Total events retrieved: {}", allEvents.size());
        return allEvents;
    }

    private List<Event> saveAll(List<Event> events) {
        List<Event> savedEvents = new ArrayList<>();

        for(Event event : events) {
            Optional<Event> optionalOldEvent = eventRepository.findOneByGoogleId(event.getGoogleId());
            if(optionalOldEvent.isPresent()) {
                Event oldEvent = optionalOldEvent.get();
                eventMapper.updateEvent(oldEvent, event);
                savedEvents.add(eventRepository.save(oldEvent));
            } else {
                savedEvents.add(eventRepository.save(event));
            }
        }

        return savedEvents;
    }

    public void setShouldSendBot(Session session, Long eventId, Boolean shouldSendBot) {
        Event event = eventRepository.findById(eventId).orElseThrow();
        if(session.getGoogleEmailAddresses().stream()
                .noneMatch(emailAddress -> Objects.equals(event.getOwner().getUsername(), emailAddress))) {
            throw new IllegalArgumentException("no rights on this eventId");
        }
        event.setShouldSendBot(shouldSendBot);
        eventRepository.save(event);
    }
}

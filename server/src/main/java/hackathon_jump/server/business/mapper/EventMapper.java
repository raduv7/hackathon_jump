package hackathon_jump.server.business.mapper;

import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import hackathon_jump.server.model.domain.Event;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface EventMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(source = "id", target = "googleId")
    @Mapping(source = "summary", target = "title")
    @Mapping(source = "location", target = "location")
    @Mapping(source = "description", target = "description")
    @Mapping(source = "htmlLink", target = "link")
    @Mapping(source = "start", target = "startDateTime", qualifiedByName = "dateTimeToLocalDateTime")
    @Mapping(source = "attendees", target = "attendees", qualifiedByName = "attendeesToStringList")
    @Mapping(source = "creator.email", target = "creator")
    @Mapping(target = "shouldSendBot", constant = "false")
    @Mapping(target = "sentBot", constant = "false")
    Event googleEventToEvent(com.google.api.services.calendar.model.Event googleEvent);

    List<Event> googleEventsToEvents(List<com.google.api.services.calendar.model.Event> googleEvents);

    default void updateEvent(Event event, Event other) {
        if(other.getAttendees() != null) {
            event.setAttendees(other.getAttendees());
        }
        if(other.getCreator() != null) {
            event.setCreator(other.getCreator());
        }
        if(other.getDescription() != null) {
            event.setDescription(other.getDescription());
        }
        if(other.getLink() != null) {
            event.setLink(other.getLink());
        }
        if(other.getLocation() != null) {
            event.setLocation(other.getLocation());
        }
        if(other.getStartDateTime() != null) {
            event.setStartDateTime(other.getStartDateTime());
        }
        if(other.getTitle() != null) {
            event.setTitle(other.getTitle());
        }
    }

    @Named("dateTimeToLocalDateTime")
    default LocalDateTime dateTimeToLocalDateTime(EventDateTime eventDateTime) {
        if (eventDateTime == null) {
            return null;
        }
        
        if (eventDateTime.getDateTime() != null) {
            return LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(eventDateTime.getDateTime().getValue()),
                ZoneId.systemDefault()
            );
        } else if (eventDateTime.getDate() != null) {
            return LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(eventDateTime.getDate().getValue()),
                ZoneId.systemDefault()
            );
        }
        
        return null;
    }

    @Named("attendeesToStringList")
    default List<String> attendeesToStringList(List<EventAttendee> attendees) {
        if (attendees == null || attendees.isEmpty()) {
            return List.of();
        }
        
        return attendees.stream()
                .map(EventAttendee::getEmail)
                .filter(email -> email != null && !email.trim().isEmpty())
                .collect(Collectors.toList());
    }
}

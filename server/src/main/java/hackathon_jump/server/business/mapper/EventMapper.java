package hackathon_jump.server.business.mapper;

import com.google.api.services.calendar.model.Event as GoogleEvent;
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

    @Mapping(source = "id", target = "googleId")
    @Mapping(source = "summary", target = "title")
    @Mapping(source = "htmlLink", target = "link")
    @Mapping(source = "start", target = "startDateTime", qualifiedByName = "dateTimeToLocalDateTime")
    @Mapping(source = "attendees", target = "attendees", qualifiedByName = "attendeesToStringList")
    @Mapping(source = "creator.email", target = "creator")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "shouldSendBot", constant = "false")
    @Mapping(target = "sentBot", constant = "false")
    Event googleEventToEvent(GoogleEvent googleEvent);

    List<Event> googleEventsToEvents(List<GoogleEvent> googleEvents);

    @Named("dateTimeToLocalDateTime")
    default LocalDateTime dateTimeToLocalDateTime(GoogleEvent.DateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        
        if (dateTime.getDateTime() != null) {
            return LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(dateTime.getDateTime().getValue()),
                ZoneId.systemDefault()
            );
        } else if (dateTime.getDate() != null) {
            return LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(dateTime.getDate().getValue()),
                ZoneId.systemDefault()
            );
        }
        
        return null;
    }

    @Named("attendeesToStringList")
    default List<String> attendeesToStringList(List<GoogleEvent.Attendee> attendees) {
        if (attendees == null || attendees.isEmpty()) {
            return List.of();
        }
        
        return attendees.stream()
                .map(GoogleEvent.Attendee::getEmail)
                .filter(email -> email != null && !email.trim().isEmpty())
                .collect(Collectors.toList());
    }
}

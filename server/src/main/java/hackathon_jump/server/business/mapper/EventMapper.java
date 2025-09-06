package hackathon_jump.server.business.mapper;

import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import hackathon_jump.server.model.domain.Event;
import hackathon_jump.server.model.domain.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface EventMapper {
    static final Pattern MEET_PATTERN = Pattern.compile("https://meet\\.google\\.com/[a-z\\-]+");
    static final Pattern ZOOM_PATTERN = Pattern.compile("https://(?:[a-z0-9]+\\.)?zoom\\.us/j/\\d+");
    static final Pattern TEAMS_PATTERN = Pattern.compile("https://teams\\.microsoft\\.com/l/meetup-join/[^ \t\n]+");

    @Mapping(target = "id", ignore = true)
    @Mapping(source = "id", target = "googleId")
    @Mapping(source = "summary", target = "title")
    @Mapping(source = "location", target = "location")
    @Mapping(source = "description", target = "description")
    @Mapping(source = "googleEvent", target = "link", qualifiedByName = "extractMeetingLink")
    @Mapping(source = "start", target = "startDateTime", qualifiedByName = "dateTimeToLocalDateTime")
    @Mapping(source = "attendees", target = "attendees", qualifiedByName = "attendeesToStringList")
    @Mapping(source = "creator.email", target = "creator")
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "shouldSendBot", constant = "false")
    @Mapping(target = "sentBot", constant = "false")
    Event googleEventToEvent(com.google.api.services.calendar.model.Event googleEvent);

    List<Event> googleEventsToEvents(List<com.google.api.services.calendar.model.Event> googleEvents);
    
    default List<Event> googleEventsToEvents(List<com.google.api.services.calendar.model.Event> googleEvents, User owner) {
        return googleEvents.stream()
                .map(googleEvent -> {
                    Event event = googleEventToEvent(googleEvent);
                    event.setOwner(owner);
                    return event;
                })
                .collect(Collectors.toList());
    }

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

    @Named("extractMeetingLink")
    default String extractMeetingLink(com.google.api.services.calendar.model.Event event) {
        String meetInDescription = findMeetingLinkInText(event.getDescription());
        if(meetInDescription != null) {
            return meetInDescription;
        }
        String meetInLocation = findMeetingLinkInText(event.getLocation());
        if(meetInLocation != null) {
            return meetInLocation;
        }
        return null;
    }

    private String findMeetingLinkInText(String str) {
        if (str == null || str.trim().isEmpty()) {
            return null;
        }
        
        Matcher m = MEET_PATTERN.matcher(str);
        if (m.find()) {
            return m.group();
        }
        m = ZOOM_PATTERN.matcher(str);
        if (m.find()) {
            return m.group();
        }
        m = TEAMS_PATTERN.matcher(str);
        if (m.find()) {
            return m.group();
        }
        return null;
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

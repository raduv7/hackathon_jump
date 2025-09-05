package hackathon_jump.server.model;

import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;
import java.util.List;

@Table
@Data
@AllArgsConstructor
@EqualsAndHashCode
public class CalendarEvent {
    private String id;
    private String status;
    private String htmlLink;
    private OffsetDateTime created;
    private OffsetDateTime updated;
    private String summary;
    private String description;
    private String location;
    private OffsetDateTime start;
    private OffsetDateTime end;
    private List<String> attendees;
    private boolean useNoteTaker;
}
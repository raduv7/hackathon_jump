package hackathon_jump.server.model.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import hackathon_jump.server.model.converter.StringListConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class Event {
    @Id
    @GeneratedValue
    private Long id;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "owner_id", referencedColumnName = "id")
    private User owner;
    @Column(unique = true)
    private String googleId;
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "event_report_id", referencedColumnName = "id")
    @JsonIgnore
    private EventReport eventReport;
    private String title;
    private String description;
    private String creator;
    @Convert(converter = StringListConverter.class)
    private List<String> attendees;
    private LocalDateTime startDateTime;
    private String location;
    private String link;
    private boolean shouldSendBot;
    private boolean finished;

    public boolean canChangeBot() {
        return startDateTime.minusMinutes(owner.getMinutesBeforeMeeting()).isAfter(LocalDateTime.now());
    }

    public boolean shouldUpdateBot() {
        return link != null && shouldSendBot && canChangeBot();
    }
}

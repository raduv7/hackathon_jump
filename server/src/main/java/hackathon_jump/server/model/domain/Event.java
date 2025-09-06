package hackathon_jump.server.model.domain;

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
    private String title;
    private String description;
    private String creator;
    @Convert(converter = StringListConverter.class)
    private List<String> attendees;
    private LocalDateTime startDateTime;
    private String location;
    private String link;
    private boolean shouldSendBot;
    private boolean sentBot;
}

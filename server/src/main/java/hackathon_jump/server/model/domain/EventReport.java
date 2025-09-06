package hackathon_jump.server.model.domain;

import hackathon_jump.server.model.enums.EMeetingPlatform;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class EventReport {
    @Id
    @GeneratedValue
    private Long id;
    @Column(unique = true)
    private String botId;
    private String attendees;
    private LocalDateTime startDateTime;
    private EMeetingPlatform platform;
    private String transcript;
    private String emailText;
    private String linkedinPost;
    private String facebookPost;
    
    @OneToOne(fetch = FetchType.EAGER, mappedBy = "eventReport")
    private Event event;
}

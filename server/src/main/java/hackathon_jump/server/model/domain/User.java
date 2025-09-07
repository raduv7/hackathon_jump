package hackathon_jump.server.model.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import hackathon_jump.server.model.enums.EOauthProvider;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(uniqueConstraints =
    @UniqueConstraint(name = "unique_usernameAndProvider", columnNames = {"username", "provider"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"automations"})
public class User {
    @Id
    @GeneratedValue
    private Long id;
    private String username; // or email address in case of provider google
    private String oauthToken;
    private EOauthProvider provider;
    private Integer minutesBeforeMeeting;
    
    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "user_automation",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "automation_id")
    )
    @JsonIgnore
    private Set<Automation> automations = new HashSet<>();
}

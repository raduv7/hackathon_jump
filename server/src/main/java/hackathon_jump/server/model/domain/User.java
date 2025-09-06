package hackathon_jump.server.model.domain;

import hackathon_jump.server.model.EOauthProvider;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(uniqueConstraints =
    @UniqueConstraint(name = "unique_usernameAndProvider", columnNames = {"username", "provider"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class User {
    @Id
    @GeneratedValue
    private Long id;
    private String username; // or email address in case of provider google
    private String oauthToken;
    private EOauthProvider provider;
    private Integer minutesBeforeMeeting;
}

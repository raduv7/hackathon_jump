package hackathon_jump.server.model.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import hackathon_jump.server.model.enums.EAutomationType;
import hackathon_jump.server.model.enums.EMediaPlatform;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"users"})
public class Automation {
    @Id
    @GeneratedValue
    private Long id;
    private String title;
    private EAutomationType automationType;
    private EMediaPlatform mediaPlatform;
    private String description;
    private String example;
    
    @ManyToMany(mappedBy = "automations", fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<User> users = new HashSet<>();
}

package hackathon_jump.server.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@AllArgsConstructor
@EqualsAndHashCode
public class Session {
    private List<String> googleEmailAddresses;
    private String facebookUsername;
    private String linkedinUsername;

    @Override
    public String toString() {
        return "session {"  +
                "googleEmailAddresses: " + googleEmailAddresses.stream().reduce("", (s1, s2) -> s1 + ',' + s2) + "; " +
                "facebookUsername: " + facebookUsername + "; " +
                "linkedinUsername: " + linkedinUsername + "}";
    }
}

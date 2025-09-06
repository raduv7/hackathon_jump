package hackathon_jump.server.model.dto;

import hackathon_jump.server.model.enums.EOauthProvider;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignInResponse {
    private String token;
    private String username;
    private String name;
    private String picture;
    private EOauthProvider oauthProvider;
}

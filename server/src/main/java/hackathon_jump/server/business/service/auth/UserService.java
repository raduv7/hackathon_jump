package hackathon_jump.server.business.service.auth;

import hackathon_jump.server.infrastructure.repository.IUserRepository;
import hackathon_jump.server.model.enums.EOauthProvider;
import hackathon_jump.server.model.domain.User;
import hackathon_jump.server.model.dto.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {
    private static Logger log = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private IUserRepository userRepository;

    public void save(String username, String oauthToken, EOauthProvider provider) {
        Optional<User> optionalUser = userRepository.findByUsernameAndProvider(username, provider);
        if(optionalUser.isPresent()) {
            User user = userRepository.findByUsernameAndProvider(username, provider).orElseThrow();
            user.setOauthToken(oauthToken);
            userRepository.save(user);
        } else {
            // todo handle new minutes from fe
            User newUser = new User(null, username, oauthToken, provider, 0);
            userRepository.save(newUser);
        }
    }

    public void updateMinutesBeforeMeeting(Session session, Integer minutesBeforeMeeting) {
        for (String googleEmail : session.getGoogleEmailAddresses()) {
            User user = userRepository.findByUsernameAndProvider(googleEmail, EOauthProvider.GOOGLE).orElseThrow();
            user.setMinutesBeforeMeeting(minutesBeforeMeeting);
            userRepository.save(user);
        }
    }
}

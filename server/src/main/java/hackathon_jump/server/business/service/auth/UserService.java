package hackathon_jump.server.business.service.auth;

import hackathon_jump.server.business.service.calendar.EventReportService;
import hackathon_jump.server.infrastructure.repository.IEventRepository;
import hackathon_jump.server.infrastructure.repository.IUserRepository;
import hackathon_jump.server.model.domain.Automation;
import hackathon_jump.server.model.domain.Event;
import hackathon_jump.server.model.enums.EOauthProvider;
import hackathon_jump.server.model.domain.User;
import hackathon_jump.server.model.dto.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;

@Service
public class UserService {
    private static Logger log = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private IUserRepository userRepository;
    @Autowired
    private IEventRepository eventRepository;
    @Autowired
    private EventReportService eventReportService;

    public void save(String username, String oauthToken, EOauthProvider provider) {
        Optional<User> optionalUser = userRepository.findByUsernameAndProvider(username, provider);
        if(optionalUser.isPresent()) {
            User user = userRepository.findByUsernameAndProvider(username, provider).orElseThrow();
            user.setOauthToken(oauthToken);
            userRepository.save(user);
        } else {
            User newUser = new User(null, username, oauthToken, provider, 0, new HashSet<>());
            userRepository.save(newUser);
        }
    }

    public Integer getMinutesBeforeMeeting(Session session) {
        User user = this.userRepository.findByUsernameAndProvider(session.getGoogleEmailAddresses().getFirst(), EOauthProvider.GOOGLE).orElseThrow();
        return user.getMinutesBeforeMeeting();
    }

    public void updateMinutesBeforeMeeting(Session session, Integer minutesBeforeMeeting) {
        for (String googleEmail : session.getGoogleEmailAddresses()) {
            User user = userRepository.findByUsernameAndProvider(googleEmail, EOauthProvider.GOOGLE).orElseThrow();
            user.setMinutesBeforeMeeting(minutesBeforeMeeting);
            userRepository.save(user);

            for(Event event : this.eventRepository.findAllByOwner(user)) {
                if(event.shouldUpdateBot()) {
                    this.eventReportService.updateBot(event);
                }
            }
        }
    }

    @Transactional
    public void syncUsers(String user1Email, String user2Email) {
        log.info("Starting user synchronization from {} to {}", user1Email, user2Email);
        
        Optional<User> user1Opt = userRepository.findByUsernameAndProvider(user1Email, EOauthProvider.GOOGLE);
        Optional<User> user2Opt = userRepository.findByUsernameAndProvider(user2Email, EOauthProvider.GOOGLE);
        
        if (user1Opt.isEmpty()) {
            log.error("Source user not found: {}", user1Email);
            throw new RuntimeException("Source user not found: " + user1Email);
        }
        
        if (user2Opt.isEmpty()) {
            log.error("Target user not found: {}", user2Email);
            throw new RuntimeException("Target user not found: " + user2Email);
        }
        
        User user1 = user1Opt.get();
        User user2 = user2Opt.get();
        
        log.info("Found users - Source: {} (ID: {}), Target: {} (ID: {})", 
                user1Email, user1.getId(), user2Email, user2.getId());
        
        Integer originalMinutes = user2.getMinutesBeforeMeeting();
        user2.setMinutesBeforeMeeting(user1.getMinutesBeforeMeeting());
        log.info("Copied minutes before meeting: {} -> {} (was: {})", 
                user1.getMinutesBeforeMeeting(), user2.getMinutesBeforeMeeting(), originalMinutes);
        
        int removedCount = user2.getAutomations().size();
        user2.getAutomations().clear();
        log.info("Removed {} existing automations from user2", removedCount);
        
        int copiedCount = 0;
        for (Automation automation : user1.getAutomations()) {
            user2.getAutomations().add(automation);
            copiedCount++;
        }
        log.info("Copied {} automations from user1 to user2", copiedCount);
        
        userRepository.save(user2);
        
        int updatedBots = 0;
        for (Event event : this.eventRepository.findAllByOwner(user2)) {
            if (event.shouldUpdateBot()) {
                this.eventReportService.updateBot(event);
                updatedBots++;
            }
        }
        log.info("Updated {} event bots for user2", updatedBots);
        
        log.info("User synchronization completed successfully. User {} now has {} automations and {} minutes before meeting", 
                user2Email, user2.getAutomations().size(), user2.getMinutesBeforeMeeting());
    }
}

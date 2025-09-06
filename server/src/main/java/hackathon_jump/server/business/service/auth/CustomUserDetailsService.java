package hackathon_jump.server.business.service.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    @Deprecated
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // todo
        // For now, create a simple user details object
        // In a real application, you would fetch this from your database
        return org.springframework.security.core.userdetails.User.builder()
                .username(email)
                .password("") // OAuth users don't have passwords
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                .build();
    }
}

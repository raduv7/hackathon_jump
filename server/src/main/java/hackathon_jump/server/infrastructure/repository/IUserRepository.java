package hackathon_jump.server.infrastructure.repository;

import hackathon_jump.server.model.EOauthProvider;
import hackathon_jump.server.model.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IUserRepository extends JpaRepository<User, Long> {
    boolean existsByUsernameAndProvider(String username, EOauthProvider provider);
    Optional<User> findByUsernameAndProvider(String username, EOauthProvider provider);
}

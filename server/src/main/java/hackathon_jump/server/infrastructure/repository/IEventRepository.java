package hackathon_jump.server.infrastructure.repository;

import hackathon_jump.server.model.domain.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IEventRepository extends JpaRepository<Event, Long> {
    Optional<Event> findOneByGoogleId(String googleId);
}

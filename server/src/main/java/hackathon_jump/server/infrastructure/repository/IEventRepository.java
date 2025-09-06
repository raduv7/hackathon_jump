package hackathon_jump.server.infrastructure.repository;

import hackathon_jump.server.model.domain.Event;
import hackathon_jump.server.model.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface IEventRepository extends JpaRepository<Event, Long> {
    List<Event> findAllByOwner(User owner);
    List<Event> findAllByOwnerAndFinishedIsFalseAndStartDateTimeAfter(User owner, LocalDateTime dateTime);

    Optional<Event> findOneByGoogleId(String googleId);
}

package hackathon_jump.server.infrastructure.repository;

import hackathon_jump.server.model.domain.EventReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IEventReportRepository extends JpaRepository<EventReport, Long> {
    List<EventReport> findAllByPlatformIsNull(); // used for polling not yet finished bots
    List<EventReport> findAllByPlatformIsNotNull(); // used for displaying data

    Optional<EventReport> findByBotId(String botId);
}

package hackathon_jump.server.infrastructure.repository;

import hackathon_jump.server.model.domain.Automation;
import hackathon_jump.server.model.domain.EventReport;
import hackathon_jump.server.model.domain.EventReportAutomation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IEventReportAutomationRepository extends JpaRepository<EventReportAutomation, Long> {
    
    /**
     * Find EventReportAutomation by automation and event report
     */
    Optional<EventReportAutomation> findByAutomationAndEventReport(Automation automation, EventReport eventReport);
    
    /**
     * Find all EventReportAutomation by automation
     */
    List<EventReportAutomation> findByAutomation(Automation automation);
    
    /**
     * Find all EventReportAutomation by automation ID
     */
    List<EventReportAutomation> findByAutomation_Id(Long automationId);
    
    /**
     * Find all EventReportAutomation by event report
     */
    List<EventReportAutomation> findByEventReport(EventReport eventReport);
    
    /**
     * Find all EventReportAutomation by event report ID
     */
    List<EventReportAutomation> findByEventReport_Id(Long eventReportId);
    
    /**
     * Delete all EventReportAutomation by automation ID
     */
    void deleteByAutomation_Id(Long automationId);
    
    /**
     * Delete all EventReportAutomation by event report ID
     */
    void deleteByEventReport_Id(Long eventReportId);
    
    /**
     * Check if EventReportAutomation exists for given automation and event report
     */
    boolean existsByAutomationAndEventReport(Automation automation, EventReport eventReport);
}

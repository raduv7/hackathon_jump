package hackathon_jump.server.model.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Data
@Table
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class EventReportAutomation {
    @Id
    @GeneratedValue
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_report_id", referencedColumnName = "id")
    private EventReport eventReport;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "automation_id", referencedColumnName = "id")
    private Automation automation;
    private String title;
    private String text;
}

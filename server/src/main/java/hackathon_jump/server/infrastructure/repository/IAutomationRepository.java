package hackathon_jump.server.infrastructure.repository;

import hackathon_jump.server.model.domain.Automation;
import hackathon_jump.server.model.domain.User;
import hackathon_jump.server.model.enums.EAutomationType;
import hackathon_jump.server.model.enums.EMediaPlatform;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IAutomationRepository extends JpaRepository<Automation, Long> {
    
    // Find automations by user
    List<Automation> findByUsers(User user);
    
    // Find automation by ID and user (to ensure user has access)
    @Query("SELECT a FROM Automation a JOIN a.users u WHERE a.id = :automationId AND u.id = :userId")
    Optional<Automation> findByIdAndUserId(@Param("automationId") Long automationId, @Param("userId") Long userId);
    
    // Find automations by type
    List<Automation> findByAutomationType(EAutomationType automationType);
    
    // Find automations by media platform
    List<Automation> findByMediaPlatform(EMediaPlatform mediaPlatform);
    
    // Find automations by type and platform
    List<Automation> findByAutomationTypeAndMediaPlatform(EAutomationType automationType, EMediaPlatform mediaPlatform);
    
    // Find user's automations by type
    @Query("SELECT a FROM Automation a JOIN a.users u WHERE u.id = :userId AND a.automationType = :automationType")
    List<Automation> findByUserIdAndAutomationType(@Param("userId") Long userId, @Param("automationType") EAutomationType automationType);
    
    // Find user's automations by platform
    @Query("SELECT a FROM Automation a JOIN a.users u WHERE u.id = :userId AND a.mediaPlatform = :mediaPlatform")
    List<Automation> findByUserIdAndMediaPlatform(@Param("userId") Long userId, @Param("mediaPlatform") EMediaPlatform mediaPlatform);
}

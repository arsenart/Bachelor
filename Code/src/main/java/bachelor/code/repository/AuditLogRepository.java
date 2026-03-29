package bachelor.code.repository;

import bachelor.code.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByEntityTypeAndEntityIdOrderByPerformedAtAsc(String entityType, Long entityId);

    List<AuditLog> findAllByOrderByPerformedAtDesc();
}

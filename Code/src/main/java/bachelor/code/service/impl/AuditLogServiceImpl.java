package bachelor.code.service.impl;

import bachelor.code.entity.AuditLog;
import bachelor.code.entity.User;
import bachelor.code.repository.AuditLogRepository;
import bachelor.code.service.AuditLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogServiceImpl(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    // REQUIRES_NEW ensures audit is saved even if the caller's transaction rolls back
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String entityType, Long entityId, String action,
                    User performedBy, String oldValue, String newValue, String comment) {
        auditLogRepository.save(new AuditLog(entityType, entityId, action,
                performedBy, oldValue, newValue, comment));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> getForEntity(String entityType, Long entityId) {
        return auditLogRepository.findByEntityTypeAndEntityIdOrderByPerformedAtAsc(entityType, entityId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> getAll() {
        return auditLogRepository.findAllByOrderByPerformedAtDesc();
    }
}

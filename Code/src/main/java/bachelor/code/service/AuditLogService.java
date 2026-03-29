package bachelor.code.service;

import bachelor.code.entity.AuditLog;
import bachelor.code.entity.User;

import java.util.List;

public interface AuditLogService {

    void log(String entityType, Long entityId, String action,
             User performedBy, String oldValue, String newValue, String comment);

    List<AuditLog> getForEntity(String entityType, Long entityId);

    List<AuditLog> getAll();
}

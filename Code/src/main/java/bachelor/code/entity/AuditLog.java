package bachelor.code.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_entity", columnList = "entity_type, entity_id")
})
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(nullable = false, length = 100)
    private String action;

    // null when action is system-generated (e.g. scheduled task)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by_id")
    private User performedBy;

    @Column(length = 255)
    private String oldValue;

    @Column(length = 255)
    private String newValue;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(nullable = false)
    private LocalDateTime performedAt;

    public AuditLog() {}

    public AuditLog(String entityType, Long entityId, String action,
                    User performedBy, String oldValue, String newValue, String comment) {
        this.entityType = entityType;
        this.entityId = entityId;
        this.action = action;
        this.performedBy = performedBy;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.comment = comment;
        this.performedAt = LocalDateTime.now();
    }

    // Getters — no setters: AuditLog is immutable after creation

    public Long getId() { return id; }
    public String getEntityType() { return entityType; }
    public Long getEntityId() { return entityId; }
    public String getAction() { return action; }
    public User getPerformedBy() { return performedBy; }
    public String getOldValue() { return oldValue; }
    public String getNewValue() { return newValue; }
    public String getComment() { return comment; }
    public LocalDateTime getPerformedAt() { return performedAt; }
}

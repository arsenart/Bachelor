package bachelor.code.entity;

import bachelor.code.enums.StepStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "approval_steps")
public class ApprovalStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private ApprovalRequest request;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_id", nullable = false)
    private User approver;

    @Column(nullable = false)
    private int stepOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StepStatus status = StepStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String comment;

    private LocalDateTime decidedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public ApprovalStep() {}

    public ApprovalStep(ApprovalRequest request, User approver, int stepOrder) {
        this.request = request;
        this.approver = approver;
        this.stepOrder = stepOrder;
        this.status = StepStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and setters

    public Long getId() { return id; }

    public ApprovalRequest getRequest() { return request; }
    public void setRequest(ApprovalRequest request) { this.request = request; }

    public User getApprover() { return approver; }
    public void setApprover(User approver) { this.approver = approver; }

    public int getStepOrder() { return stepOrder; }
    public void setStepOrder(int stepOrder) { this.stepOrder = stepOrder; }

    public StepStatus getStatus() { return status; }
    public void setStatus(StepStatus status) { this.status = status; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public LocalDateTime getDecidedAt() { return decidedAt; }
    public void setDecidedAt(LocalDateTime decidedAt) { this.decidedAt = decidedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}

package bachelor.code.entity;

import bachelor.code.enums.RequestType;
import bachelor.code.enums.RoleType;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "approval_rules")
public class ApprovalRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestType requestType;

    // Amount range for which this rule applies (null = no bound)
    @Column(precision = 15, scale = 2)
    private BigDecimal minAmount;

    @Column(precision = 15, scale = 2)
    private BigDecimal maxAmount;

    // Specific user approver — takes precedence over approverRole if set
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_id")
    private User approver;

    // Role-based approver — used when approver is null
    @Enumerated(EnumType.STRING)
    private RoleType approverRole;

    // Order within a multi-step route (1 = first)
    @Column(nullable = false)
    private int stepOrder = 1;

    @Column(nullable = false)
    private boolean active = true;

    @Column(length = 255)
    private String description;

    public ApprovalRule() {}

    // Getters and setters

    public Long getId() { return id; }

    public RequestType getRequestType() { return requestType; }
    public void setRequestType(RequestType requestType) { this.requestType = requestType; }

    public BigDecimal getMinAmount() { return minAmount; }
    public void setMinAmount(BigDecimal minAmount) { this.minAmount = minAmount; }

    public BigDecimal getMaxAmount() { return maxAmount; }
    public void setMaxAmount(BigDecimal maxAmount) { this.maxAmount = maxAmount; }

    public User getApprover() { return approver; }
    public void setApprover(User approver) { this.approver = approver; }

    public RoleType getApproverRole() { return approverRole; }
    public void setApproverRole(RoleType approverRole) { this.approverRole = approverRole; }

    public int getStepOrder() { return stepOrder; }
    public void setStepOrder(int stepOrder) { this.stepOrder = stepOrder; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}

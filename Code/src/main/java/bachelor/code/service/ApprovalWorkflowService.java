package bachelor.code.service;

import bachelor.code.entity.ApprovalRequest;
import bachelor.code.entity.User;

import java.util.List;

public interface ApprovalWorkflowService {

    void approve(Long requestId, User approver, String comment);

    void reject(Long requestId, User approver, String comment);

    void returnForRevision(Long requestId, User approver, String comment);

    // All requests with a PENDING step assigned to this approver
    List<ApprovalRequest> getPendingForApprover(User approver);

    long countPendingForApprover(User approver);
}

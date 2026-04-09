package bachelor.code.service;

import bachelor.code.dto.CreateApprovalRequestDto;
import bachelor.code.entity.ApprovalRequest;
import bachelor.code.entity.User;
import bachelor.code.enums.RequestStatus;

import java.util.List;

public interface ApprovalRequestService {

    ApprovalRequest createDraft(CreateApprovalRequestDto dto, User requester);

    ApprovalRequest updateDraft(Long requestId, CreateApprovalRequestDto dto, User requester);

    // Submit for approval: builds route, creates steps, changes status to PENDING_APPROVAL
    ApprovalRequest submit(Long requestId, User requester);

    ApprovalRequest getById(Long id);

    // Full detail including steps and approvers (avoids N+1)
    ApprovalRequest getByIdWithDetails(Long id);

    List<ApprovalRequest> getByRequester(User requester);

    List<ApprovalRequest> getAll();

    long countByRequesterAndStatus(User requester, RequestStatus status);

    void markAsRealized(Long requestId, User user);

    void markAsClosed(Long requestId, User user);
}

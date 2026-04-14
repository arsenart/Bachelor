package bachelor.code.service.impl;

import bachelor.code.dto.CreateApprovalRequestDto;
import bachelor.code.entity.ApprovalRequest;
import bachelor.code.entity.ApprovalStep;
import bachelor.code.entity.User;
import bachelor.code.enums.RequestStatus;
import bachelor.code.exception.BusinessRuleViolationException;
import bachelor.code.exception.ResourceNotFoundException;
import bachelor.code.repository.ApprovalRequestRepository;
import bachelor.code.service.ApprovalRequestService;
import bachelor.code.service.ApprovalRoutingService;
import bachelor.code.service.AuditLogService;
import bachelor.code.service.EmailService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ApprovalRequestServiceImpl implements ApprovalRequestService {

    private final ApprovalRequestRepository requestRepository;
    private final ApprovalRoutingService routingService;
    private final AuditLogService auditLogService;
    private final EmailService emailService;

    public ApprovalRequestServiceImpl(ApprovalRequestRepository requestRepository,
                                      ApprovalRoutingService routingService,
                                      AuditLogService auditLogService,
                                      EmailService emailService) {
        this.requestRepository = requestRepository;
        this.routingService = routingService;
        this.auditLogService = auditLogService;
        this.emailService = emailService;
    }

    @Override
    @Transactional
    public ApprovalRequest createDraft(CreateApprovalRequestDto dto, User requester) {
        ApprovalRequest request = new ApprovalRequest();
        applyDto(request, dto);
        request.setRequestedBy(requester);
        request.setStatus(RequestStatus.NEW);

        ApprovalRequest saved = requestRepository.save(request);

        auditLogService.log("ApprovalRequest", saved.getId(), "CREATED",
                requester, null, RequestStatus.NEW.name(), null);

        return saved;
    }

    @Override
    @Transactional
    public ApprovalRequest updateDraft(Long requestId, CreateApprovalRequestDto dto, User requester) {
        ApprovalRequest request = getById(requestId);

        if (!request.isEditable()) {
            throw new BusinessRuleViolationException(
                    "Request #" + requestId + " cannot be edited in status: " + request.getStatus());
        }
        if (!request.getRequestedBy().getId().equals(requester.getId())) {
            throw new BusinessRuleViolationException("You can only edit your own requests.");
        }

        String oldStatus = request.getStatus().name();
        applyDto(request, dto);
        ApprovalRequest saved = requestRepository.save(request);

        auditLogService.log("ApprovalRequest", saved.getId(), "UPDATED",
                requester, oldStatus, saved.getStatus().name(), null);

        return saved;
    }

    @Override
    @Transactional
    public ApprovalRequest submit(Long requestId, User requester) {
        ApprovalRequest request = getById(requestId);

        if (!request.isSubmittable()) {
            throw new BusinessRuleViolationException(
                    "Request #" + requestId + " cannot be submitted in status: " + request.getStatus());
        }
        if (!request.getRequestedBy().getId().equals(requester.getId())) {
            throw new BusinessRuleViolationException("You can only submit your own requests.");
        }

        // If re-submitting after RETURNED, clear old steps (cascade delete handles DB)
        request.getSteps().clear();

        // Build approval route
        List<ApprovalStep> steps = routingService.buildStepsForRequest(request);
        steps.forEach(step -> request.getSteps().add(step));

        request.setStatus(RequestStatus.PENDING_APPROVAL);
        ApprovalRequest saved = requestRepository.save(request);

        auditLogService.log("ApprovalRequest", saved.getId(), "SUBMITTED",
                requester, RequestStatus.NEW.name(), RequestStatus.PENDING_APPROVAL.name(), null);

        // Notify first approver
        ApprovalStep firstStep = saved.getActiveStep();
        if (firstStep != null) {
            emailService.sendApprovalNeededEmail(
                    firstStep.getApprover().getEmail(),
                    requester.getFirstName() + " " + requester.getLastName(),
                    saved.getTitle(),
                    saved.getAmount(),
                    saved.getCurrency());
        }

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public ApprovalRequest getById(Long id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Request #" + id + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public ApprovalRequest getByIdWithDetails(Long id) {
        return requestRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Request #" + id + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApprovalRequest> getByRequester(User requester) {
        return requestRepository.findByRequestedByOrderByCreatedAtDesc(requester);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApprovalRequest> getAll() {
        return requestRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public long countByRequesterAndStatus(User requester, RequestStatus status) {
        return requestRepository.countByRequestedByAndStatus(requester, status);
    }

    @Override
    @Transactional
    public void markAsRealized(Long requestId, User user) {
        ApprovalRequest request = getById(requestId);
        if (request.getStatus() != RequestStatus.APPROVED) {
            throw new BusinessRuleViolationException(
                    "Request #" + requestId + " must be APPROVED before marking as realized.");
        }
        request.setStatus(RequestStatus.REALIZED);
        requestRepository.save(request);
        auditLogService.log("ApprovalRequest", requestId, "REALIZED",
                user, RequestStatus.APPROVED.name(), RequestStatus.REALIZED.name(), null);
    }

    @Override
    @Transactional
    public void markAsClosed(Long requestId, User user) {
        ApprovalRequest request = getById(requestId);
        if (request.getStatus() != RequestStatus.REALIZED) {
            throw new BusinessRuleViolationException(
                    "Request #" + requestId + " must be REALIZED before closing.");
        }
        request.setStatus(RequestStatus.CLOSED);
        requestRepository.save(request);
        auditLogService.log("ApprovalRequest", requestId, "CLOSED",
                user, RequestStatus.REALIZED.name(), RequestStatus.CLOSED.name(), null);
    }

    private void applyDto(ApprovalRequest request, CreateApprovalRequestDto dto) {
        request.setTitle(dto.getTitle());
        request.setType(dto.getType());
        request.setAmount(dto.getAmount());
        request.setCurrency(dto.getCurrency() != null ? dto.getCurrency() : "CZK");
        request.setSupplier(dto.getSupplier());
        request.setDescription(dto.getDescription());
        request.setJustification(dto.getJustification());
        request.setDepartment(dto.getDepartment());
        request.setRequestedDate(dto.getRequestedDate());
        request.setDocumentLink(dto.getDocumentLink());
    }
}

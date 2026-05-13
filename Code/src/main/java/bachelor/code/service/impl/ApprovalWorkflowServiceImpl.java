package bachelor.code.service.impl;

import bachelor.code.entity.ApprovalRequest;
import bachelor.code.entity.ApprovalStep;
import bachelor.code.entity.User;
import bachelor.code.enums.RequestStatus;
import bachelor.code.enums.StepStatus;
import bachelor.code.exception.BusinessRuleViolationException;
import bachelor.code.exception.ResourceNotFoundException;
import bachelor.code.repository.ApprovalRequestRepository;
import bachelor.code.repository.ApprovalStepRepository;
import bachelor.code.service.ApprovalWorkflowService;
import bachelor.code.service.AuditLogService;
import bachelor.code.service.EmailService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ApprovalWorkflowServiceImpl implements ApprovalWorkflowService {

    private final ApprovalRequestRepository requestRepository;
    private final ApprovalStepRepository stepRepository;
    private final AuditLogService auditLogService;
    private final EmailService emailService;

    public ApprovalWorkflowServiceImpl(ApprovalRequestRepository requestRepository,
                                       ApprovalStepRepository stepRepository,
                                       AuditLogService auditLogService,
                                       EmailService emailService) {
        this.requestRepository = requestRepository;
        this.stepRepository = stepRepository;
        this.auditLogService = auditLogService;
        this.emailService = emailService;
    }

    @Override
    @Transactional
    public void approve(Long requestId, User approver, String comment) {
        ApprovalRequest request = loadAndValidate(requestId, approver);
        ApprovalStep activeStep = request.getActiveStep();

        activeStep.setStatus(StepStatus.APPROVED);
        activeStep.setComment(comment);
        activeStep.setDecidedAt(LocalDateTime.now());
        stepRepository.save(activeStep);

        auditLogService.log("ApprovalRequest", requestId, "STEP_APPROVED",
                approver, StepStatus.PENDING.name(), StepStatus.APPROVED.name(), comment);

        // Check if there are more PENDING steps
        boolean hasNextStep = request.getSteps().stream()
                .anyMatch(s -> s.getStatus() == StepStatus.PENDING
                        && s.getStepOrder() > activeStep.getStepOrder());

        if (hasNextStep) {
            // Notify next approver
            ApprovalStep nextStep = request.getActiveStep();
            if (nextStep != null) {
                emailService.sendApprovalNeededEmail(
                        nextStep.getApprover().getEmail(),
                        request.getRequestedBy().getFirstName() + " " + request.getRequestedBy().getLastName(),
                        request.getTitle(),
                        request.getAmount(),
                        request.getCurrency());
            }
        } else {
            // All steps approved → request is APPROVED
            request.setStatus(RequestStatus.APPROVED);
            requestRepository.save(request);

            auditLogService.log("ApprovalRequest", requestId, "FULLY_APPROVED",
                    approver, RequestStatus.PENDING_APPROVAL.name(), RequestStatus.APPROVED.name(), null);

            emailService.sendRequestDecisionEmail(
                    request.getRequestedBy().getEmail(),
                    request.getTitle(),
                    "APPROVED",
                    comment);
        }
    }

    @Override
    @Transactional
    public void reject(Long requestId, User approver, String comment) {
        if (comment == null || comment.isBlank()) {
            throw new BusinessRuleViolationException("Komentář je povinný při zamítnutí žádosti.");
        }

        ApprovalRequest request = loadAndValidate(requestId, approver);
        ApprovalStep activeStep = request.getActiveStep();

        activeStep.setStatus(StepStatus.REJECTED);
        activeStep.setComment(comment);
        activeStep.setDecidedAt(LocalDateTime.now());
        stepRepository.save(activeStep);

        request.setStatus(RequestStatus.REJECTED);
        requestRepository.save(request);

        auditLogService.log("ApprovalRequest", requestId, "REJECTED",
                approver, RequestStatus.PENDING_APPROVAL.name(), RequestStatus.REJECTED.name(), comment);

        emailService.sendRequestDecisionEmail(
                request.getRequestedBy().getEmail(),
                request.getTitle(),
                "REJECTED",
                comment);
    }

    @Override
    @Transactional
    public void returnForRevision(Long requestId, User approver, String comment) {
        if (comment == null || comment.isBlank()) {
            throw new BusinessRuleViolationException("Komentář je povinný při vrácení žádosti k revizi.");
        }

        ApprovalRequest request = loadAndValidate(requestId, approver);
        ApprovalStep activeStep = request.getActiveStep();

        activeStep.setStatus(StepStatus.RETURNED_FOR_REVISION);
        activeStep.setComment(comment);
        activeStep.setDecidedAt(LocalDateTime.now());
        stepRepository.save(activeStep);

        request.setStatus(RequestStatus.RETURNED_FOR_REVISION);
        requestRepository.save(request);

        auditLogService.log("ApprovalRequest", requestId, "RETURNED_FOR_REVISION",
                approver, RequestStatus.PENDING_APPROVAL.name(), RequestStatus.RETURNED_FOR_REVISION.name(), comment);

        emailService.sendRequestDecisionEmail(
                request.getRequestedBy().getEmail(),
                request.getTitle(),
                "RETURNED FOR REVISION",
                comment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApprovalRequest> getPendingForApprover(User approver) {
        return stepRepository.findRequestsByApproverAndStatus(approver, StepStatus.PENDING);
    }

    @Override
    @Transactional(readOnly = true)
    public long countPendingForApprover(User approver) {
        return stepRepository.countDistinctRequestsByApproverAndStatus(approver, StepStatus.PENDING);
    }

    // Load request, verify it's in PENDING_APPROVAL, verify the caller is the active step's approver
    private ApprovalRequest loadAndValidate(Long requestId, User approver) {
        ApprovalRequest request = requestRepository.findByIdWithDetails(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request #" + requestId + " not found"));

        if (request.getStatus() != RequestStatus.PENDING_APPROVAL) {
            throw new BusinessRuleViolationException(
                    "Žádost #" + requestId + " není ve stavu čekající na schválení (stav: " + request.getStatus() + ").");
        }

        ApprovalStep activeStep = request.getActiveStep();
        if (activeStep == null) {
            throw new BusinessRuleViolationException("Pro žádost #" + requestId + " nebyl nalezen aktivní krok schválení.");
        }
        if (!activeStep.getApprover().getId().equals(approver.getId())) {
            throw new BusinessRuleViolationException("Nejste určeným schvalovatelem pro tento krok.");
        }

        return request;
    }
}

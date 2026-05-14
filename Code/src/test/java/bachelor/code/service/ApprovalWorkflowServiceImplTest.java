package bachelor.code.service;

import bachelor.code.entity.ApprovalRequest;
import bachelor.code.entity.ApprovalStep;
import bachelor.code.entity.User;
import bachelor.code.enums.RequestStatus;
import bachelor.code.enums.RequestType;
import bachelor.code.enums.StepStatus;
import bachelor.code.exception.BusinessRuleViolationException;
import bachelor.code.repository.ApprovalRequestRepository;
import bachelor.code.repository.ApprovalStepRepository;
import bachelor.code.service.impl.ApprovalWorkflowServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit testy pro {@link ApprovalWorkflowServiceImpl} -- ověřují, že
 * schvalovací akce (approve, reject, returnForRevision) korektně mění
 * stav žádosti, postupují k~dalšímu kroku víceúrovňového workflow
 * a~provádějí povinné validace.
 */
@ExtendWith(MockitoExtension.class)
class ApprovalWorkflowServiceImplTest {

    @Mock
    private ApprovalRequestRepository requestRepository;
    @Mock
    private ApprovalStepRepository stepRepository;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private ApprovalWorkflowServiceImpl workflowService;

    private User requester;
    private User approver1;
    private User approver2;
    private User otherUser;

    @BeforeEach
    void setUp() {
        requester = makeUser(1L, "req@medicton.com");
        approver1 = makeUser(2L, "step1@medicton.com");
        approver2 = makeUser(3L, "step2@medicton.com");
        otherUser = makeUser(99L, "other@medicton.com");
    }

    @Test
    @DisplayName("Schválení jediného kroku → žádost je APPROVED, žadatel je notifikován")
    void approvingTheOnlyStepMarksRequestAsApproved() {
        ApprovalRequest req = makeRequestWithSteps(List.of(makeStep(approver1, 1)));
        when(requestRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(req));

        workflowService.approve(10L, approver1, "OK");

        ApprovalStep step = req.getSteps().get(0);
        assertThat(step.getStatus()).isEqualTo(StepStatus.APPROVED);
        assertThat(step.getDecidedAt()).isNotNull();
        assertThat(req.getStatus()).isEqualTo(RequestStatus.APPROVED);

        verify(emailService).sendRequestDecisionEmail(
                eq(requester.getEmail()), anyString(), eq("APPROVED"), anyString());
        verify(emailService, never()).sendApprovalNeededEmail(
                anyString(), anyString(), anyString(), any(), anyString());
    }

    @Test
    @DisplayName("Schválení prvního kroku dvoukrokové žádosti → notifikace dalšímu schvalovateli")
    void approvingFirstOfTwoStepsNotifiesNextApprover() {
        ApprovalStep s1 = makeStep(approver1, 1);
        ApprovalStep s2 = makeStep(approver2, 2);
        ApprovalRequest req = makeRequestWithSteps(List.of(s1, s2));

        when(requestRepository.findByIdWithDetails(20L)).thenReturn(Optional.of(req));

        workflowService.approve(20L, approver1, "First step OK");

        assertThat(s1.getStatus()).isEqualTo(StepStatus.APPROVED);
        assertThat(s2.getStatus()).isEqualTo(StepStatus.PENDING);
        assertThat(req.getStatus()).isEqualTo(RequestStatus.PENDING_APPROVAL);

        verify(emailService).sendApprovalNeededEmail(
                eq(approver2.getEmail()), anyString(), anyString(), any(), anyString());
        verify(emailService, never()).sendRequestDecisionEmail(
                anyString(), anyString(), eq("APPROVED"), anyString());
    }

    @Test
    @DisplayName("Zamítnutí libovolného kroku → celá žádost je REJECTED")
    void rejectionMarksWholeRequestAsRejected() {
        ApprovalStep s1 = makeStep(approver1, 1);
        ApprovalStep s2 = makeStep(approver2, 2);
        ApprovalRequest req = makeRequestWithSteps(List.of(s1, s2));

        when(requestRepository.findByIdWithDetails(30L)).thenReturn(Optional.of(req));

        workflowService.reject(30L, approver1, "Důvod zamítnutí");

        assertThat(s1.getStatus()).isEqualTo(StepStatus.REJECTED);
        assertThat(req.getStatus()).isEqualTo(RequestStatus.REJECTED);

        verify(emailService).sendRequestDecisionEmail(
                eq(requester.getEmail()), anyString(), eq("REJECTED"), eq("Důvod zamítnutí"));
    }

    @Test
    @DisplayName("Zamítnutí bez komentáře → BusinessRuleViolationException")
    void rejectionWithoutCommentThrows() {
        assertThatThrownBy(() -> workflowService.reject(40L, approver1, ""))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Komentář je povinný");

        verify(requestRepository, never()).findByIdWithDetails(anyLong());
    }

    @Test
    @DisplayName("Vrácení k revizi bez komentáře → BusinessRuleViolationException")
    void returnForRevisionWithoutCommentThrows() {
        assertThatThrownBy(() -> workflowService.returnForRevision(50L, approver1, null))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Komentář je povinný");
    }

    @Test
    @DisplayName("Vrácení k revizi → žádost přejde do RETURNED_FOR_REVISION")
    void returnForRevisionChangesStatus() {
        ApprovalRequest req = makeRequestWithSteps(List.of(makeStep(approver1, 1)));
        when(requestRepository.findByIdWithDetails(60L)).thenReturn(Optional.of(req));

        workflowService.returnForRevision(60L, approver1, "Doplňte čísla");

        assertThat(req.getStatus()).isEqualTo(RequestStatus.RETURNED_FOR_REVISION);
        verify(emailService).sendRequestDecisionEmail(
                eq(requester.getEmail()), anyString(),
                eq("RETURNED FOR REVISION"), eq("Doplňte čísla"));
    }

    @Test
    @DisplayName("Schvalovatel není určen pro aktivní krok → BusinessRuleViolationException")
    void wrongApproverIsBlocked() {
        ApprovalRequest req = makeRequestWithSteps(List.of(makeStep(approver1, 1)));
        when(requestRepository.findByIdWithDetails(70L)).thenReturn(Optional.of(req));

        assertThatThrownBy(() -> workflowService.approve(70L, otherUser, "OK"))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Nejste určeným schvalovatelem");
    }

    @Test
    @DisplayName("Žádost není v PENDING_APPROVAL → BusinessRuleViolationException")
    void approvingNonPendingRequestIsBlocked() {
        ApprovalRequest req = makeRequestWithSteps(List.of(makeStep(approver1, 1)));
        req.setStatus(RequestStatus.APPROVED); // already approved
        when(requestRepository.findByIdWithDetails(80L)).thenReturn(Optional.of(req));

        assertThatThrownBy(() -> workflowService.approve(80L, approver1, "OK"))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("není ve stavu čekající na schválení");
    }

    @Test
    @DisplayName("Audit log je vždy zapsán při změně stavu")
    void auditLogIsAlwaysWritten() {
        ApprovalRequest req = makeRequestWithSteps(List.of(makeStep(approver1, 1)));
        when(requestRepository.findByIdWithDetails(90L)).thenReturn(Optional.of(req));

        workflowService.approve(90L, approver1, "OK");

        // Při jednokrokovém schválení se loguje 2× -- STEP_APPROVED + FULLY_APPROVED
        verify(auditLogService, times(2)).log(
                eq("ApprovalRequest"), eq(90L), anyString(), eq(approver1),
                anyString(), anyString(), any());
    }

    // ── Test helpers ─────────────────────────────────────────────

    private User makeUser(Long id, String email) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        u.setFirstName("First");
        u.setLastName("Last");
        u.setActive(true);
        return u;
    }

    private ApprovalStep makeStep(User approver, int order) {
        ApprovalStep s = new ApprovalStep();
        s.setApprover(approver);
        s.setStepOrder(order);
        s.setStatus(StepStatus.PENDING);
        return s;
    }

    private ApprovalRequest makeRequestWithSteps(List<ApprovalStep> steps) {
        ApprovalRequest r = new ApprovalRequest();
        r.setType(RequestType.EXPENSE);
        r.setAmount(new BigDecimal("5000"));
        r.setStatus(RequestStatus.PENDING_APPROVAL);
        r.setRequestedBy(requester);
        r.setTitle("Test žádost");
        r.setSteps(new ArrayList<>(steps));
        return r;
    }
}

package bachelor.code.service;

import bachelor.code.entity.ApprovalRequest;
import bachelor.code.entity.ApprovalRule;
import bachelor.code.entity.ApprovalStep;
import bachelor.code.entity.User;
import bachelor.code.enums.RequestType;
import bachelor.code.enums.RoleType;
import bachelor.code.exception.BusinessRuleViolationException;
import bachelor.code.repository.ApprovalRuleRepository;
import bachelor.code.repository.UserRepository;
import bachelor.code.service.impl.ApprovalRoutingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit testy pro {@link ApprovalRoutingServiceImpl} -- klíčovou servisní
 * třídu, která určuje schvalovatele na základě konfigurovatelných pravidel
 * v tabulce approval_rules. Testy ověřují korektnost směrování pro různé
 * kombinace typu žádosti a výše částky, ochranu proti self-approval
 * a chování při nekonfigurovaných pravidlech.
 */
@ExtendWith(MockitoExtension.class)
class ApprovalRoutingServiceImplTest {

    @Mock
    private ApprovalRuleRepository ruleRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ApprovalRoutingServiceImpl routingService;

    private User requester;
    private User approverL1;
    private User approverL2;
    private User approverL3;

    @BeforeEach
    void setUp() {
        requester = makeUser(1L, "requester@medicton.com", RoleType.REQUESTER);
        approverL1 = makeUser(2L, "lead@medicton.com", RoleType.APPROVER);
        approverL2 = makeUser(3L, "cfo@medicton.com", RoleType.APPROVER);
        approverL3 = makeUser(4L, "ceo@medicton.com", RoleType.APPROVER);
    }

    @Nested
    @DisplayName("Routing EXPENSE — výběr schvalovatele podle částky")
    class ExpenseRouting {

        @Test
        @DisplayName("L1: výdaj 2 500 Kč → jednostupňové schválení rolí APPROVER")
        void smallExpenseRoutesToSingleApprover() {
            ApprovalRequest req = makeRequest(RequestType.EXPENSE, new BigDecimal("2500"));
            ApprovalRule rule = makeRoleRule(RequestType.EXPENSE,
                    new BigDecimal("0"), new BigDecimal("3000"), 1, RoleType.APPROVER);

            when(ruleRepository.findByRequestTypeAndActiveTrueOrderByStepOrderAsc(RequestType.EXPENSE))
                    .thenReturn(List.of(rule));
            when(userRepository.findFirstByRolesContainingAndActiveTrueAndIdNot(
                    eq(RoleType.APPROVER), any()))
                    .thenReturn(Optional.of(approverL1));

            List<ApprovalStep> steps = routingService.buildStepsForRequest(req);

            assertThat(steps).hasSize(1);
            assertThat(steps.get(0).getApprover()).isEqualTo(approverL1);
            assertThat(steps.get(0).getStepOrder()).isEqualTo(1);
        }

        @Test
        @DisplayName("L3: výdaj 75 000 Kč → dvoustupňové schválení (CFO + CEO)")
        void largeExpenseRoutesToTwoApprovers() {
            ApprovalRequest req = makeRequest(RequestType.EXPENSE, new BigDecimal("75000"));
            ApprovalRule step1 = makeUserRule(RequestType.EXPENSE,
                    new BigDecimal("50001"), null, 1, approverL2);
            ApprovalRule step2 = makeUserRule(RequestType.EXPENSE,
                    new BigDecimal("50001"), null, 2, approverL3);

            when(ruleRepository.findByRequestTypeAndActiveTrueOrderByStepOrderAsc(RequestType.EXPENSE))
                    .thenReturn(List.of(step1, step2));

            List<ApprovalStep> steps = routingService.buildStepsForRequest(req);

            assertThat(steps).hasSize(2);
            assertThat(steps.get(0).getApprover()).isEqualTo(approverL2);
            assertThat(steps.get(1).getApprover()).isEqualTo(approverL3);
        }

        @Test
        @DisplayName("Pravidlo mimo rozsah částky se neaktivuje")
        void ruleOutsideAmountRangeIsFiltered() {
            ApprovalRequest req = makeRequest(RequestType.EXPENSE, new BigDecimal("2500"));
            ApprovalRule matchingRule = makeRoleRule(RequestType.EXPENSE,
                    new BigDecimal("0"), new BigDecimal("3000"), 1, RoleType.APPROVER);
            ApprovalRule nonMatchingRule = makeUserRule(RequestType.EXPENSE,
                    new BigDecimal("50001"), null, 1, approverL3);

            when(ruleRepository.findByRequestTypeAndActiveTrueOrderByStepOrderAsc(RequestType.EXPENSE))
                    .thenReturn(List.of(matchingRule, nonMatchingRule));
            when(userRepository.findFirstByRolesContainingAndActiveTrueAndIdNot(any(), any()))
                    .thenReturn(Optional.of(approverL1));

            List<ApprovalStep> steps = routingService.buildStepsForRequest(req);

            assertThat(steps).hasSize(1);
            assertThat(steps.get(0).getApprover()).isEqualTo(approverL1);
        }
    }

    @Nested
    @DisplayName("Routing PURCHASE — vždy dvoustupňové schválení")
    class PurchaseRouting {

        @Test
        @DisplayName("PURCHASE: dva přímo přiřazení schvalovatelé (Matera + jednatel)")
        void purchaseAlwaysRoutesToTwoApprovers() {
            ApprovalRequest req = makeRequest(RequestType.PURCHASE, new BigDecimal("15000"));
            ApprovalRule step1 = makeUserRule(RequestType.PURCHASE, null, null, 1, approverL2);
            ApprovalRule step2 = makeUserRule(RequestType.PURCHASE, null, null, 2, approverL3);

            when(ruleRepository.findByRequestTypeAndActiveTrueOrderByStepOrderAsc(RequestType.PURCHASE))
                    .thenReturn(List.of(step1, step2));

            List<ApprovalStep> steps = routingService.buildStepsForRequest(req);

            assertThat(steps).hasSize(2);
            assertThat(steps.get(0).getStepOrder()).isEqualTo(1);
            assertThat(steps.get(1).getStepOrder()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Validace a chybové stavy")
    class ErrorHandling {

        @Test
        @DisplayName("Žádné pravidlo pro typ + částku → BusinessRuleViolationException")
        void noMatchingRuleThrowsException() {
            ApprovalRequest req = makeRequest(RequestType.EXPENSE, new BigDecimal("2500"));

            when(ruleRepository.findByRequestTypeAndActiveTrueOrderByStepOrderAsc(RequestType.EXPENSE))
                    .thenReturn(List.of());

            assertThatThrownBy(() -> routingService.buildStepsForRequest(req))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("No approval rule");
        }

        @Test
        @DisplayName("Pravidlo bez schvalovatele i bez role → BusinessRuleViolationException")
        void ruleWithoutApproverNorRoleThrowsException() {
            ApprovalRequest req = makeRequest(RequestType.EXPENSE, new BigDecimal("2500"));
            ApprovalRule brokenRule = new ApprovalRule();
            brokenRule.setRequestType(RequestType.EXPENSE);
            brokenRule.setMinAmount(new BigDecimal("0"));
            brokenRule.setMaxAmount(new BigDecimal("3000"));
            brokenRule.setStepOrder(1);
            brokenRule.setActive(true);
            // ani approver, ani approverRole nejsou nastaveny

            when(ruleRepository.findByRequestTypeAndActiveTrueOrderByStepOrderAsc(RequestType.EXPENSE))
                    .thenReturn(List.of(brokenRule));

            assertThatThrownBy(() -> routingService.buildStepsForRequest(req))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("nemá nastaveného schvalovatele");
        }

        @Test
        @DisplayName("Žadatel je jediný uživatel s rolí APPROVER → self-approval check brání směrování")
        void selfApprovalIsBlocked() {
            // Žadatel sám má roli APPROVER
            User selfApprover = makeUser(99L, "self@medicton.com",
                    RoleType.REQUESTER, RoleType.APPROVER);
            ApprovalRequest req = new ApprovalRequest();
            req.setType(RequestType.EXPENSE);
            req.setAmount(new BigDecimal("2500"));
            req.setRequestedBy(selfApprover);

            ApprovalRule rule = makeRoleRule(RequestType.EXPENSE,
                    new BigDecimal("0"), new BigDecimal("3000"), 1, RoleType.APPROVER);

            when(ruleRepository.findByRequestTypeAndActiveTrueOrderByStepOrderAsc(RequestType.EXPENSE))
                    .thenReturn(List.of(rule));
            // Repository vyloučí požadatele a vrátí Optional.empty() — žádný jiný approver neexistuje
            when(userRepository.findFirstByRolesContainingAndActiveTrueAndIdNot(
                    eq(RoleType.APPROVER), eq(99L)))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> routingService.buildStepsForRequest(req))
                    .isInstanceOf(BusinessRuleViolationException.class);
        }

        @Test
        @DisplayName("Konkrétně přiřazený schvalovatel je sám žadatelem → blokováno")
        void directApproverEqualsRequesterIsBlocked() {
            ApprovalRequest req = makeRequest(RequestType.EXPENSE, new BigDecimal("25000"));
            // Pravidlo nastavené tak, že schvalovatel = žadatel
            ApprovalRule rule = makeUserRule(RequestType.EXPENSE,
                    new BigDecimal("3001"), new BigDecimal("50000"), 1, requester);

            when(ruleRepository.findByRequestTypeAndActiveTrueOrderByStepOrderAsc(RequestType.EXPENSE))
                    .thenReturn(List.of(rule));

            assertThatThrownBy(() -> routingService.buildStepsForRequest(req))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("stejný jako žadatel");
        }
    }

    // ── Test helpers ─────────────────────────────────────────────

    private User makeUser(Long id, String email, RoleType... roles) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        u.setFirstName("Test");
        u.setLastName("User");
        u.setActive(true);
        Set<RoleType> roleSet = new HashSet<>();
        for (RoleType r : roles) roleSet.add(r);
        u.setRoles(roleSet);
        return u;
    }

    private ApprovalRequest makeRequest(RequestType type, BigDecimal amount) {
        ApprovalRequest r = new ApprovalRequest();
        r.setType(type);
        r.setAmount(amount);
        r.setRequestedBy(requester);
        return r;
    }

    private ApprovalRule makeRoleRule(RequestType type, BigDecimal min, BigDecimal max,
                                      int order, RoleType role) {
        ApprovalRule r = new ApprovalRule();
        r.setRequestType(type);
        r.setMinAmount(min);
        r.setMaxAmount(max);
        r.setStepOrder(order);
        r.setApproverRole(role);
        r.setActive(true);
        return r;
    }

    private ApprovalRule makeUserRule(RequestType type, BigDecimal min, BigDecimal max,
                                      int order, User approver) {
        ApprovalRule r = new ApprovalRule();
        r.setRequestType(type);
        r.setMinAmount(min);
        r.setMaxAmount(max);
        r.setStepOrder(order);
        r.setApprover(approver);
        r.setActive(true);
        return r;
    }
}

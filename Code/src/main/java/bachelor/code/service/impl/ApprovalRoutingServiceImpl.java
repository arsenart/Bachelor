package bachelor.code.service.impl;

import bachelor.code.entity.ApprovalRequest;
import bachelor.code.entity.ApprovalRule;
import bachelor.code.entity.ApprovalStep;
import bachelor.code.entity.User;
import bachelor.code.exception.BusinessRuleViolationException;
import bachelor.code.repository.ApprovalRuleRepository;
import bachelor.code.repository.UserRepository;
import bachelor.code.service.ApprovalRoutingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ApprovalRoutingServiceImpl implements ApprovalRoutingService {

    private final ApprovalRuleRepository ruleRepository;
    private final UserRepository userRepository;

    public ApprovalRoutingServiceImpl(ApprovalRuleRepository ruleRepository,
                                      UserRepository userRepository) {
        this.ruleRepository = ruleRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApprovalStep> buildStepsForRequest(ApprovalRequest request) {
        List<ApprovalRule> matchingRules = ruleRepository
                .findByRequestTypeAndActiveTrueOrderByStepOrderAsc(request.getType())
                .stream()
                .filter(rule -> amountIsInRange(request.getAmount(), rule))
                .collect(Collectors.toList());

        if (matchingRules.isEmpty()) {
            throw new BusinessRuleViolationException(
                    "No approval rule configured for type " + request.getType()
                    + " with amount " + request.getAmount() + " " + request.getCurrency()
                    + ". Please contact the administrator.");
        }

        return matchingRules.stream()
                .map(rule -> {
                    User approver = resolveApprover(rule, request.getRequestedBy());
                    ApprovalStep step = new ApprovalStep(request, approver, rule.getStepOrder());
                    return step;
                })
                .collect(Collectors.toList());
    }

    private boolean amountIsInRange(BigDecimal amount, ApprovalRule rule) {
        boolean aboveMin = rule.getMinAmount() == null
                || amount.compareTo(rule.getMinAmount()) >= 0;
        boolean belowMax = rule.getMaxAmount() == null
                || amount.compareTo(rule.getMaxAmount()) <= 0;
        return aboveMin && belowMax;
    }

    private User resolveApprover(ApprovalRule rule, User requester) {
        User approver;

        if (rule.getApprover() != null) {
            approver = rule.getApprover();
        } else if (rule.getApproverRole() != null) {
            approver = userRepository
                    .findFirstByRolesContainingAndActiveTrueAndIdNot(rule.getApproverRole(), requester.getId())
                    .orElseThrow(() -> new BusinessRuleViolationException(
                            "Nelze najít schvalovatele pro tuto žádost. "
                            + "Kontaktujte administrátora pro úpravu pravidel schvalování."));
        } else {
            throw new BusinessRuleViolationException(
                    "ApprovalRule (id=" + rule.getId() + ") nemá nastaveného schvalovatele ani roli.");
        }

        // Requester cannot be their own approver (for direct approver_id rules)
        if (approver.getId().equals(requester.getId())) {
            throw new BusinessRuleViolationException(
                    "Nakonfigurovaný schvalovatel je stejný jako žadatel. "
                    + "Kontaktujte administrátora pro úpravu pravidel schvalování.");
        }

        return approver;
    }
}

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
                    .findFirstByRolesContainingAndActiveTrue(rule.getApproverRole())
                    .orElseThrow(() -> new BusinessRuleViolationException(
                            "No active user with role " + rule.getApproverRole()
                            + " found to process this request."));
        } else {
            throw new BusinessRuleViolationException(
                    "ApprovalRule (id=" + rule.getId() + ") has neither approver nor approverRole set.");
        }

        // Requester cannot be their own approver
        if (approver.getId().equals(requester.getId())) {
            throw new BusinessRuleViolationException(
                    "The configured approver is the same as the requester. "
                    + "Please contact the administrator to update the approval rules.");
        }

        return approver;
    }
}

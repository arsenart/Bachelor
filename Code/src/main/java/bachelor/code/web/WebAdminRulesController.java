package bachelor.code.web;

import bachelor.code.entity.ApprovalRule;
import bachelor.code.entity.User;
import bachelor.code.enums.RequestType;
import bachelor.code.enums.RoleType;
import bachelor.code.repository.ApprovalRuleRepository;
import bachelor.code.repository.UserRepository;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/admin/rules")
public class WebAdminRulesController {

    private final ApprovalRuleRepository ruleRepository;
    private final UserRepository userRepository;
    private final MessageSource messageSource;

    public WebAdminRulesController(ApprovalRuleRepository ruleRepository,
                                    UserRepository userRepository,
                                    MessageSource messageSource) {
        this.ruleRepository = ruleRepository;
        this.userRepository = userRepository;
        this.messageSource = messageSource;
    }

    @GetMapping
    public String rulesPage(Model model) {
        model.addAttribute("rules", ruleRepository.findAllByOrderByRequestTypeAscStepOrderAsc());
        model.addAttribute("requestTypes", RequestType.values());
        model.addAttribute("approverRoles", RoleType.values());
        model.addAttribute("approvers", userRepository.findByRolesContainingAndActiveTrue(RoleType.APPROVER));
        return "admin/rules";
    }

    @PostMapping
    public String createRule(@RequestParam RequestType requestType,
                             @RequestParam(required = false) BigDecimal minAmount,
                             @RequestParam(required = false) BigDecimal maxAmount,
                             @RequestParam(required = false) Long approverId,
                             @RequestParam(required = false) RoleType approverRole,
                             @RequestParam int stepOrder,
                             @RequestParam(required = false) String description,
                             RedirectAttributes ra) {
        ApprovalRule rule = new ApprovalRule();
        rule.setRequestType(requestType);
        rule.setMinAmount(minAmount);
        rule.setMaxAmount(maxAmount);
        rule.setStepOrder(stepOrder);
        rule.setDescription(description);
        rule.setActive(true);

        if (approverId != null) {
            User approver = userRepository.findById(approverId).orElse(null);
            rule.setApprover(approver);
        } else {
            rule.setApproverRole(approverRole);
        }

        ruleRepository.save(rule);
        ra.addFlashAttribute("success",
                messageSource.getMessage("flash.rule.created", null, LocaleContextHolder.getLocale()));
        return "redirect:/admin/rules";
    }

    @PostMapping("/{id}/toggle")
    public String toggleRule(@PathVariable Long id, RedirectAttributes ra) {
        ApprovalRule rule = ruleRepository.findById(id).orElse(null);
        if (rule != null) {
            rule.setActive(!rule.isActive());
            ruleRepository.save(rule);
        }
        ra.addFlashAttribute("success",
                messageSource.getMessage("flash.rule.toggled", null, LocaleContextHolder.getLocale()));
        return "redirect:/admin/rules";
    }

    @PostMapping("/{id}/delete")
    public String deleteRule(@PathVariable Long id, RedirectAttributes ra) {
        ruleRepository.deleteById(id);
        ra.addFlashAttribute("success",
                messageSource.getMessage("flash.rule.deleted", null, LocaleContextHolder.getLocale()));
        return "redirect:/admin/rules";
    }
}

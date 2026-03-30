package bachelor.code.web;

import bachelor.code.dto.ApprovalDecisionDto;
import bachelor.code.entity.ApprovalRequest;
import bachelor.code.entity.User;
import bachelor.code.exception.BusinessRuleViolationException;
import bachelor.code.service.ApprovalWorkflowService;
import bachelor.code.service.AuditLogService;
import bachelor.code.service.ApprovalRequestService;
import bachelor.code.service.UserService;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/approvals")
public class WebApprovalController {

    private final ApprovalWorkflowService workflowService;
    private final ApprovalRequestService requestService;
    private final AuditLogService auditLogService;
    private final UserService userService;
    private final MessageSource messageSource;

    public WebApprovalController(ApprovalWorkflowService workflowService,
                                 ApprovalRequestService requestService,
                                 AuditLogService auditLogService,
                                 UserService userService,
                                 MessageSource messageSource) {
        this.workflowService = workflowService;
        this.requestService = requestService;
        this.auditLogService = auditLogService;
        this.userService = userService;
        this.messageSource = messageSource;
    }

    @GetMapping
    public String pendingApprovals(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User currentUser = userService.findByEmail(userDetails.getUsername());
        model.addAttribute("requests", workflowService.getPendingForApprover(currentUser));
        return "approvals/list";
    }

    @GetMapping("/{id}")
    public String approvalDetail(@PathVariable Long id, Model model) {
        ApprovalRequest request = requestService.getByIdWithDetails(id);
        model.addAttribute("request", request);
        model.addAttribute("auditLog", auditLogService.getForEntity("ApprovalRequest", id));
        model.addAttribute("decisionDto", new ApprovalDecisionDto());
        return "approvals/detail";
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id,
                          @ModelAttribute ApprovalDecisionDto dto,
                          @AuthenticationPrincipal UserDetails userDetails,
                          RedirectAttributes redirectAttributes) {
        User currentUser = userService.findByEmail(userDetails.getUsername());
        try {
            workflowService.approve(id, currentUser, dto.getComment());
            redirectAttributes.addFlashAttribute("success",
                messageSource.getMessage("flash.approval.approved",
                    new Object[]{id}, LocaleContextHolder.getLocale()));
        } catch (BusinessRuleViolationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/approvals/" + id;
        }
        return "redirect:/approvals";
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id,
                         @ModelAttribute ApprovalDecisionDto dto,
                         @AuthenticationPrincipal UserDetails userDetails,
                         RedirectAttributes redirectAttributes) {
        User currentUser = userService.findByEmail(userDetails.getUsername());
        try {
            workflowService.reject(id, currentUser, dto.getComment());
            redirectAttributes.addFlashAttribute("success",
                messageSource.getMessage("flash.approval.rejected",
                    new Object[]{id}, LocaleContextHolder.getLocale()));
        } catch (BusinessRuleViolationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/approvals/" + id;
        }
        return "redirect:/approvals";
    }

    @PostMapping("/{id}/return")
    public String returnForRevision(@PathVariable Long id,
                                    @ModelAttribute ApprovalDecisionDto dto,
                                    @AuthenticationPrincipal UserDetails userDetails,
                                    RedirectAttributes redirectAttributes) {
        User currentUser = userService.findByEmail(userDetails.getUsername());
        try {
            workflowService.returnForRevision(id, currentUser, dto.getComment());
            redirectAttributes.addFlashAttribute("success",
                messageSource.getMessage("flash.approval.returned",
                    new Object[]{id}, LocaleContextHolder.getLocale()));
        } catch (BusinessRuleViolationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/approvals/" + id;
        }
        return "redirect:/approvals";
    }
}

package bachelor.code.web;

import bachelor.code.entity.User;
import bachelor.code.enums.DocumentStatus;
import bachelor.code.enums.RequestStatus;
import bachelor.code.enums.RoleType;
import bachelor.code.service.AccountingDocumentService;
import bachelor.code.service.ApprovalRequestService;
import bachelor.code.service.ApprovalWorkflowService;
import bachelor.code.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebDashboardController {

    private final UserService userService;
    private final ApprovalRequestService requestService;
    private final ApprovalWorkflowService workflowService;
    private final AccountingDocumentService documentService;

    public WebDashboardController(UserService userService,
                                  ApprovalRequestService requestService,
                                  ApprovalWorkflowService workflowService,
                                  AccountingDocumentService documentService) {
        this.userService = userService;
        this.requestService = requestService;
        this.workflowService = workflowService;
        this.documentService = documentService;
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User currentUser = userService.findByEmail(userDetails.getUsername());

        boolean isAdmin = currentUser.getRoles().contains(RoleType.ADMIN);

        if (currentUser.getRoles().contains(RoleType.REQUESTER) || isAdmin) {
            model.addAttribute("myNewCount",
                    requestService.countByRequesterAndStatus(currentUser, RequestStatus.NEW));
            model.addAttribute("myPendingCount",
                    requestService.countByRequesterAndStatus(currentUser, RequestStatus.PENDING_APPROVAL));
            model.addAttribute("myReturnedCount",
                    requestService.countByRequesterAndStatus(currentUser, RequestStatus.RETURNED_FOR_REVISION));
            model.addAttribute("myApprovedCount",
                    requestService.countByRequesterAndStatus(currentUser, RequestStatus.APPROVED));
        }

        if (currentUser.getRoles().contains(RoleType.APPROVER) || isAdmin) {
            model.addAttribute("pendingApprovalsCount",
                    workflowService.countPendingForApprover(currentUser));
        }

        if (currentUser.getRoles().contains(RoleType.ACCOUNTANT) || isAdmin) {
            model.addAttribute("pendingDocumentsCount",
                    documentService.countPendingForAccountant());
        }

        if (currentUser.getRoles().contains(RoleType.REQUESTER)
                || currentUser.getRoles().contains(RoleType.ACCOUNTANT)
                || isAdmin) {
            model.addAttribute("myDocsNewCount",
                    documentService.countBySubmitterAndStatus(currentUser, DocumentStatus.NEW));
            model.addAttribute("myDocsPostedCount",
                    documentService.countBySubmitterAndStatus(currentUser, DocumentStatus.POSTED));
            model.addAttribute("myDocsPaidCount",
                    documentService.countBySubmitterAndStatus(currentUser, DocumentStatus.PAID));
        }

        model.addAttribute("currentUser", currentUser);
        return "dashboard";
    }
}

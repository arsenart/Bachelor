package bachelor.code.web;

import bachelor.code.entity.User;
import bachelor.code.enums.RequestStatus;
import bachelor.code.enums.RoleType;
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

    public WebDashboardController(UserService userService,
                                  ApprovalRequestService requestService,
                                  ApprovalWorkflowService workflowService) {
        this.userService = userService;
        this.requestService = requestService;
        this.workflowService = workflowService;
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User currentUser = userService.findByEmail(userDetails.getUsername());

        if (currentUser.getRoles().contains(RoleType.REQUESTER)) {
            model.addAttribute("myNewCount",
                    requestService.countByRequesterAndStatus(currentUser, RequestStatus.NEW));
            model.addAttribute("myPendingCount",
                    requestService.countByRequesterAndStatus(currentUser, RequestStatus.PENDING_APPROVAL));
            model.addAttribute("myReturnedCount",
                    requestService.countByRequesterAndStatus(currentUser, RequestStatus.RETURNED_FOR_REVISION));
            model.addAttribute("myApprovedCount",
                    requestService.countByRequesterAndStatus(currentUser, RequestStatus.APPROVED));
        }

        if (currentUser.getRoles().contains(RoleType.APPROVER)) {
            model.addAttribute("pendingApprovalsCount",
                    workflowService.countPendingForApprover(currentUser));
        }

        model.addAttribute("currentUser", currentUser);
        return "dashboard";
    }
}

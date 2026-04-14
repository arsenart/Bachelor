package bachelor.code.web;

import bachelor.code.dto.CreateApprovalRequestDto;
import bachelor.code.entity.ApprovalRequest;
import bachelor.code.entity.User;
import bachelor.code.enums.RequestType;
import bachelor.code.exception.BusinessRuleViolationException;
import bachelor.code.service.ApprovalRequestService;
import bachelor.code.service.AuditLogService;
import bachelor.code.service.UserService;
import jakarta.validation.Valid;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/requests")
public class WebRequestController {

    private final ApprovalRequestService requestService;
    private final AuditLogService auditLogService;
    private final UserService userService;
    private final MessageSource messageSource;

    public WebRequestController(ApprovalRequestService requestService,
                                AuditLogService auditLogService,
                                UserService userService,
                                MessageSource messageSource) {
        this.requestService = requestService;
        this.auditLogService = auditLogService;
        this.userService = userService;
        this.messageSource = messageSource;
    }

    @GetMapping
    public String listRequests(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User currentUser = userService.findByEmail(userDetails.getUsername());
        model.addAttribute("requests", requestService.getByRequester(currentUser));
        return "requests/list";
    }

    @GetMapping("/new")
    public String newRequestForm(Model model) {
        model.addAttribute("requestDto", new CreateApprovalRequestDto());
        model.addAttribute("requestTypes", RequestType.values());
        return "requests/new";
    }

    @PostMapping
    public String createRequest(@Valid @ModelAttribute("requestDto") CreateApprovalRequestDto dto,
                                BindingResult bindingResult,
                                @AuthenticationPrincipal UserDetails userDetails,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("requestTypes", RequestType.values());
            return "requests/new";
        }

        User currentUser = userService.findByEmail(userDetails.getUsername());
        ApprovalRequest saved = requestService.createDraft(dto, currentUser);

        redirectAttributes.addFlashAttribute("success",
            messageSource.getMessage("flash.request.created",
                new Object[]{saved.getTitle()},
                LocaleContextHolder.getLocale()));
        return "redirect:/requests/" + saved.getId();
    }

    @GetMapping("/{id}")
    public String requestDetail(@PathVariable Long id,
                                @AuthenticationPrincipal UserDetails userDetails,
                                Model model) {
        ApprovalRequest request = requestService.getByIdWithDetails(id);
        User currentUser = userService.findByEmail(userDetails.getUsername());

        model.addAttribute("request", request);
        model.addAttribute("auditLog", auditLogService.getForEntity("ApprovalRequest", id));
        model.addAttribute("isOwner", request.getRequestedBy().getId().equals(currentUser.getId()));
        return "requests/detail";
    }

    @GetMapping("/{id}/edit")
    public String editRequestForm(@PathVariable Long id,
                                  @AuthenticationPrincipal UserDetails userDetails,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        ApprovalRequest request = requestService.getById(id);
        User currentUser = userService.findByEmail(userDetails.getUsername());

        if (!request.getRequestedBy().getId().equals(currentUser.getId())) {
            redirectAttributes.addFlashAttribute("error", "You can only edit your own requests.");
            return "redirect:/requests";
        }
        if (!request.isEditable()) {
            redirectAttributes.addFlashAttribute("error", "This request cannot be edited in its current status.");
            return "redirect:/requests/" + id;
        }

        CreateApprovalRequestDto dto = new CreateApprovalRequestDto();
        dto.setTitle(request.getTitle());
        dto.setType(request.getType());
        dto.setAmount(request.getAmount());
        dto.setCurrency(request.getCurrency());
        dto.setSupplier(request.getSupplier());
        dto.setDescription(request.getDescription());
        dto.setJustification(request.getJustification());
        dto.setDepartment(request.getDepartment());
        dto.setRequestedDate(request.getRequestedDate());
        dto.setDocumentLink(request.getDocumentLink());

        model.addAttribute("requestDto", dto);
        model.addAttribute("requestId", id);
        model.addAttribute("requestTypes", RequestType.values());
        return "requests/new";
    }

    @PostMapping("/{id}/edit")
    public String updateRequest(@PathVariable Long id,
                                @Valid @ModelAttribute("requestDto") CreateApprovalRequestDto dto,
                                BindingResult bindingResult,
                                @AuthenticationPrincipal UserDetails userDetails,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("requestId", id);
            model.addAttribute("requestTypes", RequestType.values());
            return "requests/new";
        }

        User currentUser = userService.findByEmail(userDetails.getUsername());
        requestService.updateDraft(id, dto, currentUser);

        redirectAttributes.addFlashAttribute("success",
            messageSource.getMessage("flash.request.updated", null, LocaleContextHolder.getLocale()));
        return "redirect:/requests/" + id;
    }

    @PostMapping("/{id}/submit")
    public String submitRequest(@PathVariable Long id,
                                @AuthenticationPrincipal UserDetails userDetails,
                                RedirectAttributes redirectAttributes) {
        User currentUser = userService.findByEmail(userDetails.getUsername());
        try {
            requestService.submit(id, currentUser);
            redirectAttributes.addFlashAttribute("success",
                messageSource.getMessage("flash.request.submitted", null, LocaleContextHolder.getLocale()));
        } catch (BusinessRuleViolationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/requests/" + id;
    }

    @PostMapping("/{id}/realize")
    public String realizeRequest(@PathVariable Long id,
                                  @AuthenticationPrincipal UserDetails userDetails,
                                  RedirectAttributes redirectAttributes) {
        User currentUser = userService.findByEmail(userDetails.getUsername());
        try {
            requestService.markAsRealized(id, currentUser);
            redirectAttributes.addFlashAttribute("success",
                messageSource.getMessage("flash.request.realized", null, LocaleContextHolder.getLocale()));
        } catch (BusinessRuleViolationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/requests/" + id;
    }

    @PostMapping("/{id}/close")
    public String closeRequest(@PathVariable Long id,
                                @AuthenticationPrincipal UserDetails userDetails,
                                RedirectAttributes redirectAttributes) {
        User currentUser = userService.findByEmail(userDetails.getUsername());
        try {
            requestService.markAsClosed(id, currentUser);
            redirectAttributes.addFlashAttribute("success",
                messageSource.getMessage("flash.request.closed", null, LocaleContextHolder.getLocale()));
        } catch (BusinessRuleViolationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/requests/" + id;
    }
}

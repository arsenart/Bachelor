package bachelor.code.web;

import bachelor.code.dto.CreateAccountingDocumentDto;
import bachelor.code.entity.AccountingDocument;
import bachelor.code.entity.User;
import bachelor.code.enums.DocumentType;
import bachelor.code.exception.BusinessRuleViolationException;
import bachelor.code.service.AccountingDocumentService;
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
@RequestMapping("/accounting")
public class WebAccountingController {

    private final AccountingDocumentService documentService;
    private final AuditLogService auditLogService;
    private final UserService userService;
    private final MessageSource messageSource;

    public WebAccountingController(AccountingDocumentService documentService,
                                    AuditLogService auditLogService,
                                    UserService userService,
                                    MessageSource messageSource) {
        this.documentService = documentService;
        this.auditLogService = auditLogService;
        this.userService = userService;
        this.messageSource = messageSource;
    }

    @GetMapping
    public String listDocuments(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User currentUser = userService.findByEmail(userDetails.getUsername());
        boolean isAccountant = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ACCOUNTANT") || a.getAuthority().equals("ROLE_ADMIN"));

        if (isAccountant) {
            model.addAttribute("pendingDocuments", documentService.getPendingForAccountant());
        }
        model.addAttribute("myDocuments", documentService.getBySubmitter(currentUser));
        model.addAttribute("isAccountant", isAccountant);
        return "accounting/list";
    }

    @GetMapping("/new")
    public String newDocumentForm(@RequestParam(required = false) Long requestId, Model model) {
        CreateAccountingDocumentDto dto = new CreateAccountingDocumentDto();
        if (requestId != null) {
            dto.setApprovalRequestId(requestId);
        }
        model.addAttribute("documentDto", dto);
        model.addAttribute("documentTypes", DocumentType.values());
        return "accounting/new";
    }

    @PostMapping
    public String createDocument(@Valid @ModelAttribute("documentDto") CreateAccountingDocumentDto dto,
                                  BindingResult bindingResult,
                                  @AuthenticationPrincipal UserDetails userDetails,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("documentTypes", DocumentType.values());
            return "accounting/new";
        }

        User currentUser = userService.findByEmail(userDetails.getUsername());
        AccountingDocument saved = documentService.create(dto, currentUser);

        redirectAttributes.addFlashAttribute("success",
                msg("flash.document.created", saved.getId()));
        return "redirect:/accounting/" + saved.getId();
    }

    @GetMapping("/{id}")
    public String documentDetail(@PathVariable Long id,
                                  @AuthenticationPrincipal UserDetails userDetails,
                                  Model model) {
        AccountingDocument doc = documentService.getById(id);
        User currentUser = userService.findByEmail(userDetails.getUsername());
        boolean isAccountant = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ACCOUNTANT") || a.getAuthority().equals("ROLE_ADMIN"));

        model.addAttribute("doc", doc);
        model.addAttribute("auditLog", auditLogService.getForEntity("AccountingDocument", id));
        model.addAttribute("isOwner", doc.getSubmittedBy().getId().equals(currentUser.getId()));
        model.addAttribute("isAccountant", isAccountant);
        return "accounting/detail";
    }

    @GetMapping("/{id}/edit")
    public String editDocumentForm(@PathVariable Long id,
                                    @AuthenticationPrincipal UserDetails userDetails,
                                    Model model,
                                    RedirectAttributes redirectAttributes) {
        AccountingDocument doc = documentService.getById(id);
        User currentUser = userService.findByEmail(userDetails.getUsername());

        if (!doc.getSubmittedBy().getId().equals(currentUser.getId())) {
            redirectAttributes.addFlashAttribute("error", "You can only edit your own documents.");
            return "redirect:/accounting";
        }
        if (!doc.isEditable()) {
            redirectAttributes.addFlashAttribute("error", "This document cannot be edited.");
            return "redirect:/accounting/" + id;
        }

        CreateAccountingDocumentDto dto = new CreateAccountingDocumentDto();
        dto.setType(doc.getType());
        dto.setAmountWithoutVat(doc.getAmountWithoutVat());
        dto.setAmountWithVat(doc.getAmountWithVat());
        dto.setSupplierIco(doc.getSupplierIco());
        dto.setSupplierName(doc.getSupplierName());
        dto.setDescription(doc.getDescription());
        dto.setJustification(doc.getJustification());
        dto.setDepartment(doc.getDepartment());
        dto.setTaxDate(doc.getTaxDate());
        dto.setDocumentLocation(doc.getDocumentLocation());
        if (doc.getApprovalRequest() != null) {
            dto.setApprovalRequestId(doc.getApprovalRequest().getId());
        }

        model.addAttribute("documentDto", dto);
        model.addAttribute("documentId", id);
        model.addAttribute("documentTypes", DocumentType.values());
        return "accounting/new";
    }

    @PostMapping("/{id}/edit")
    public String updateDocument(@PathVariable Long id,
                                  @Valid @ModelAttribute("documentDto") CreateAccountingDocumentDto dto,
                                  BindingResult bindingResult,
                                  @AuthenticationPrincipal UserDetails userDetails,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("documentId", id);
            model.addAttribute("documentTypes", DocumentType.values());
            return "accounting/new";
        }

        User currentUser = userService.findByEmail(userDetails.getUsername());
        documentService.update(id, dto, currentUser);

        redirectAttributes.addFlashAttribute("success", msg("flash.document.updated"));
        return "redirect:/accounting/" + id;
    }

    @PostMapping("/{id}/submit")
    public String submitDocument(@PathVariable Long id,
                                  @AuthenticationPrincipal UserDetails userDetails,
                                  RedirectAttributes redirectAttributes) {
        User currentUser = userService.findByEmail(userDetails.getUsername());
        try {
            documentService.submitToAccounting(id, currentUser);
            redirectAttributes.addFlashAttribute("success", msg("flash.document.submitted"));
        } catch (BusinessRuleViolationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/accounting/" + id;
    }

    @PostMapping("/{id}/return")
    public String returnForCompletion(@PathVariable Long id,
                                       @RequestParam String comment,
                                       @AuthenticationPrincipal UserDetails userDetails,
                                       RedirectAttributes redirectAttributes) {
        User currentUser = userService.findByEmail(userDetails.getUsername());
        try {
            documentService.returnForCompletion(id, currentUser, comment);
            redirectAttributes.addFlashAttribute("success", msg("flash.document.returned"));
        } catch (BusinessRuleViolationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/accounting/" + id;
    }

    @PostMapping("/{id}/post")
    public String markAsPosted(@PathVariable Long id,
                                @RequestParam(required = false) String pohodaNumber,
                                @AuthenticationPrincipal UserDetails userDetails,
                                RedirectAttributes redirectAttributes) {
        User currentUser = userService.findByEmail(userDetails.getUsername());
        try {
            documentService.markAsPosted(id, currentUser, pohodaNumber);
            redirectAttributes.addFlashAttribute("success", msg("flash.document.posted"));
        } catch (BusinessRuleViolationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/accounting/" + id;
    }

    @PostMapping("/{id}/pay")
    public String markAsPaid(@PathVariable Long id,
                              @AuthenticationPrincipal UserDetails userDetails,
                              RedirectAttributes redirectAttributes) {
        User currentUser = userService.findByEmail(userDetails.getUsername());
        try {
            documentService.markAsPaid(id, currentUser);
            redirectAttributes.addFlashAttribute("success", msg("flash.document.paid"));
        } catch (BusinessRuleViolationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/accounting/" + id;
    }

    @PostMapping("/{id}/close")
    public String closeDocument(@PathVariable Long id,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {
        User currentUser = userService.findByEmail(userDetails.getUsername());
        try {
            documentService.close(id, currentUser);
            redirectAttributes.addFlashAttribute("success", msg("flash.document.closed"));
        } catch (BusinessRuleViolationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/accounting/" + id;
    }

    private String msg(String key, Object... args) {
        return messageSource.getMessage(key, args, LocaleContextHolder.getLocale());
    }
}

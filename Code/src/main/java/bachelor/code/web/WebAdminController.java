package bachelor.code.web;

import bachelor.code.dto.CreateUserRequest;
import bachelor.code.exception.BusinessRuleViolationException;
import bachelor.code.exception.ResourceNotFoundException;
import bachelor.code.service.UserService;
import jakarta.validation.Valid;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class WebAdminController {

    private final UserService userService;
    private final MessageSource messageSource;

    public WebAdminController(UserService userService, MessageSource messageSource) {
        this.userService = userService;
        this.messageSource = messageSource;
    }

    @GetMapping("/users")
    public String usersPage(Model model) {
        model.addAttribute("users", userService.findAll());
        model.addAttribute("createRequest", new CreateUserRequest());
        model.addAttribute("roles", bachelor.code.enums.RoleType.values());
        return "admin/users";
    }

    @PostMapping("/users")
    public String createUser(@Valid @ModelAttribute("createRequest") CreateUserRequest request,
                             BindingResult bindingResult,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("users", userService.findAll());
            model.addAttribute("roles", bachelor.code.enums.RoleType.values());
            return "admin/users";
        }

        try {
            userService.createUser(request);
            redirectAttributes.addFlashAttribute("success",
                messageSource.getMessage("flash.user.created",
                    new Object[]{request.getEmail()},
                    LocaleContextHolder.getLocale()));
        } catch (BusinessRuleViolationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/resend-setup-link")
    public String resendSetupLink(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.resendSetupLink(id);
            redirectAttributes.addFlashAttribute("success",
                messageSource.getMessage("flash.user.resent", null, LocaleContextHolder.getLocale()));
        } catch (ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }
}

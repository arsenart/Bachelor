package bachelor.code.web;

import bachelor.code.dto.SetupPasswordRequest;
import bachelor.code.dto.SetupPasswordTokenValidationResponse;
import bachelor.code.exception.InvalidTokenException;
import bachelor.code.exception.TokenExpiredException;
import bachelor.code.service.PasswordSetupService;
import jakarta.validation.Valid;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class WebAuthController {

    private final PasswordSetupService passwordSetupService;
    private final MessageSource messageSource;

    public WebAuthController(PasswordSetupService passwordSetupService, MessageSource messageSource) {
        this.passwordSetupService = passwordSetupService;
        this.messageSource = messageSource;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/setup-password")
    public String setupPasswordPage(@RequestParam("token") String token, Model model) {
        SetupPasswordTokenValidationResponse validation = passwordSetupService.validateToken(token);

        model.addAttribute("token", token);
        model.addAttribute("validToken", validation.isValid());

        if (!validation.isValid()) {
            String key = "setup.error." + validation.getReason().toLowerCase();
            String msg = messageSource.getMessage(key, null, key, LocaleContextHolder.getLocale());
            model.addAttribute("error", msg);
        }

        SetupPasswordRequest setupRequest = new SetupPasswordRequest();
        setupRequest.setToken(token);
        model.addAttribute("setupRequest", setupRequest);
        return "setup-password";
    }

    @PostMapping("/setup-password")
    public String handleSetupPassword(@Valid @ModelAttribute("setupRequest") SetupPasswordRequest request,
                                      BindingResult bindingResult,
                                      Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("token", request.getToken());
            model.addAttribute("validToken", true);
            return "setup-password";
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            model.addAttribute("token", request.getToken());
            model.addAttribute("validToken", true);
            model.addAttribute("passwordMismatch",
                messageSource.getMessage("setup.error.mismatch", null, LocaleContextHolder.getLocale()));
            return "setup-password";
        }

        try {
            passwordSetupService.setupPassword(request);
            return "redirect:/login?passwordSet";
        } catch (InvalidTokenException | TokenExpiredException e) {
            model.addAttribute("token", request.getToken());
            model.addAttribute("validToken", false);
            model.addAttribute("error", e.getMessage());
            return "setup-password";
        }
    }
}

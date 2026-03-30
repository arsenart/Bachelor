package bachelor.code.web;

import bachelor.code.dto.SetupPasswordRequest;
import bachelor.code.dto.SetupPasswordTokenValidationResponse;
import bachelor.code.exception.InvalidTokenException;
import bachelor.code.exception.TokenExpiredException;
import bachelor.code.service.PasswordSetupService;
import jakarta.validation.Valid;
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

    public WebAuthController(PasswordSetupService passwordSetupService) {
        this.passwordSetupService = passwordSetupService;
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
            String reason = switch (validation.getReason()) {
                case "EXPIRED"   -> "Ссылка устарела. Обратитесь к администратору для получения новой.";
                case "USED"      -> "Пароль уже был установлен через эту ссылку.";
                case "NOT_FOUND" -> "Ссылка недействительна.";
                default          -> "Неверная ссылка.";
            };
            model.addAttribute("error", reason);
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

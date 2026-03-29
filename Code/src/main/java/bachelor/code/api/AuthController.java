package bachelor.code.api;

import bachelor.code.dto.LoginRequest;
import bachelor.code.dto.LoginResponse;
import bachelor.code.dto.SetupPasswordRequest;
import bachelor.code.dto.SetupPasswordTokenValidationResponse;
import bachelor.code.service.PasswordSetupService;
import bachelor.code.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final PasswordSetupService passwordSetupService;
    private final UserService userService;

    public AuthController(PasswordSetupService passwordSetupService, UserService userService) {
        this.passwordSetupService = passwordSetupService;
        this.userService = userService;
    }

    @GetMapping("/setup-password/validate")
    public ResponseEntity<SetupPasswordTokenValidationResponse> validateToken(@RequestParam("token") String token) {
        SetupPasswordTokenValidationResponse resp = passwordSetupService.validateToken(token);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/setup-password")
    public ResponseEntity<?> setupPassword(@Valid @RequestBody SetupPasswordRequest request) {
        passwordSetupService.setupPassword(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        boolean ok = userService.authenticate(request.getEmail(), request.getPassword());
        if (ok) return ResponseEntity.ok(new LoginResponse(true, "Login successful"));
        return ResponseEntity.status(401).body(new LoginResponse(false, "Invalid credentials or account not ready"));
    }
}

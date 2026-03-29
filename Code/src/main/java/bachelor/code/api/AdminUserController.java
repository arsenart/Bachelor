package bachelor.code.api;

import bachelor.code.dto.CreateUserRequest;
import bachelor.code.dto.UserResponse;
import bachelor.code.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserResponse created = UserResponse.from(userService.createUser(request));
        return ResponseEntity.ok(created);
    }

    @PostMapping("/{id}/resend-setup-link")
    public ResponseEntity<?> resendSetupLink(@PathVariable("id") Long id) {
        userService.resendSetupLink(id);
        return ResponseEntity.ok().build();
    }
}

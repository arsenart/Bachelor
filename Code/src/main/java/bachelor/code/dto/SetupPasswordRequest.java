package bachelor.code.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public class SetupPasswordRequest {

    @NotEmpty
    private String token;

    @NotEmpty
    @Size(min = 8, max = 128)
    private String newPassword;

    // getters and setters
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}

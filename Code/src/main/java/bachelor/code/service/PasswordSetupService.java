package bachelor.code.service;

import bachelor.code.dto.SetupPasswordRequest;
import bachelor.code.dto.SetupPasswordTokenValidationResponse;
import bachelor.code.entity.PasswordSetupToken;
import bachelor.code.entity.User;

public interface PasswordSetupService {
    PasswordSetupToken generateTokenForUser(User user);
    void invalidatePreviousTokens(User user);
    SetupPasswordTokenValidationResponse validateToken(String token);
    void setupPassword(SetupPasswordRequest request);
}

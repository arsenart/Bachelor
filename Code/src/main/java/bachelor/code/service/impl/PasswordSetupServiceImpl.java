package bachelor.code.service.impl;

import bachelor.code.dto.SetupPasswordRequest;
import bachelor.code.dto.SetupPasswordTokenValidationResponse;
import bachelor.code.entity.PasswordSetupToken;
import bachelor.code.entity.User;
import bachelor.code.exception.InvalidTokenException;
import bachelor.code.exception.TokenExpiredException;
import bachelor.code.exception.ResourceNotFoundException;
import bachelor.code.repository.PasswordSetupTokenRepository;
import bachelor.code.repository.UserRepository;
import bachelor.code.service.PasswordSetupService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PasswordSetupServiceImpl implements PasswordSetupService {

    private final PasswordSetupTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public PasswordSetupServiceImpl(PasswordSetupTokenRepository tokenRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public PasswordSetupToken generateTokenForUser(User user) {
        // invalidate previous tokens
        tokenRepository.findFirstByUserIdAndUsedFalseOrderByCreatedAtDesc(user.getId()).ifPresent(t -> {
            t.setUsed(true);
            t.setUsedAt(LocalDateTime.now());
            tokenRepository.save(t);
        });

        String tokenStr = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        PasswordSetupToken token = new PasswordSetupToken(tokenStr, user, now, now.plusHours(24));
        return tokenRepository.save(token);
    }

    @Override
    @Transactional
    public void invalidatePreviousTokens(User user) {
        tokenRepository.findFirstByUserIdAndUsedFalseOrderByCreatedAtDesc(user.getId()).ifPresent(t -> {
            t.setUsed(true);
            t.setUsedAt(LocalDateTime.now());
            tokenRepository.save(t);
        });
    }

    @Override
    public SetupPasswordTokenValidationResponse validateToken(String token) {
        return tokenRepository.findByToken(token).map(t -> {
            if (t.isUsed()) {
                return new SetupPasswordTokenValidationResponse(false, "USED");
            }
            if (t.getExpiresAt().isBefore(LocalDateTime.now())) {
                return new SetupPasswordTokenValidationResponse(false, "EXPIRED");
            }
            return new SetupPasswordTokenValidationResponse(true, null);
        }).orElseGet(() -> new SetupPasswordTokenValidationResponse(false, "NOT_FOUND"));
    }

    @Override
    @Transactional
    public void setupPassword(SetupPasswordRequest request) {
        PasswordSetupToken token = tokenRepository.findByToken(request.getToken()).orElseThrow(() -> new InvalidTokenException("Token not found"));
        if (token.isUsed()) throw new InvalidTokenException("Token already used");
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) throw new TokenExpiredException("Token expired");

        User user = token.getUser();
        if (user == null) throw new ResourceNotFoundException("Associated user not found");

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordSet(true);
        userRepository.save(user);

        token.setUsed(true);
        token.setUsedAt(LocalDateTime.now());
        tokenRepository.save(token);
    }
}

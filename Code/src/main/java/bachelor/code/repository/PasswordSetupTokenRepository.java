package bachelor.code.repository;

import bachelor.code.entity.PasswordSetupToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordSetupTokenRepository extends JpaRepository<PasswordSetupToken, Long> {
    Optional<PasswordSetupToken> findByToken(String token);
    Optional<PasswordSetupToken> findFirstByUserIdAndUsedFalseOrderByCreatedAtDesc(Long userId);
}

package bachelor.code.config;

import bachelor.code.entity.ApprovalRule;
import bachelor.code.entity.User;
import bachelor.code.enums.RequestType;
import bachelor.code.enums.RoleType;
import bachelor.code.repository.ApprovalRuleRepository;
import bachelor.code.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;

@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final ApprovalRuleRepository ruleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    public DataInitializer(UserRepository userRepository,
                           ApprovalRuleRepository ruleRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.ruleRepository = ruleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        User admin = seedAdmin();
        seedApprovalRules(admin);
    }

    private User seedAdmin() {
        return userRepository.findByEmail(adminEmail).orElseGet(() -> {
            User admin = new User(adminEmail, "Admin", "Admin", "Management",
                    Set.of(RoleType.ADMIN, RoleType.APPROVER, RoleType.ACCOUNTANT));
            admin.setPasswordHash(passwordEncoder.encode(adminPassword));
            admin.setPasswordSet(true);
            admin.setActive(true);
            User saved = userRepository.save(admin);

            log.info("==============================================");
            log.info("Default admin created:");
            log.info("  Email:    {}", adminEmail);
            log.info("  Password: {}", adminPassword);
            log.info("  URL:      http://localhost:8080/login");
            log.info("==============================================");

            return saved;
        });
    }

    /**
     * Seeds default ApprovalRules using the admin user as approver.
     * In a real setup, the admin configures rules through the UI or database.
     *
     * EXPENSE rules (Czech CZK thresholds):
     *   0 – 3,000      → step 1: admin (group manager)
     *   3,001 – 50,000 → step 1: admin (financial director)
     *   50,001+        → step 1: admin (management)
     *
     * PURCHASE rules (any amount, two steps):
     *   step 1: admin (purchase manager)
     *   step 2: admin (management)
     */
    private void seedApprovalRules(User admin) {
        if (ruleRepository.count() > 0) {
            return; // Already seeded
        }

        // EXPENSE: up to 3,000
        ruleRepository.save(rule(RequestType.EXPENSE,
                BigDecimal.ZERO, new BigDecimal("3000"),
                admin, 1, "Expense up to 3,000 CZK → Group Manager"));

        // EXPENSE: 3,001 – 50,000
        ruleRepository.save(rule(RequestType.EXPENSE,
                new BigDecimal("3001"), new BigDecimal("50000"),
                admin, 1, "Expense 3,001–50,000 CZK → Financial Director"));

        // EXPENSE: over 50,000
        ruleRepository.save(rule(RequestType.EXPENSE,
                new BigDecimal("50001"), null,
                admin, 1, "Expense over 50,000 CZK → Management"));

        // PURCHASE: step 1
        ruleRepository.save(rule(RequestType.PURCHASE,
                BigDecimal.ZERO, null,
                admin, 1, "Purchase step 1 → Purchase Manager"));

        // PURCHASE: step 2
        ruleRepository.save(rule(RequestType.PURCHASE,
                BigDecimal.ZERO, null,
                admin, 2, "Purchase step 2 → Management"));

        log.info("Default approval rules seeded (approver: {}). Update them via the database or Admin UI.", adminEmail);
    }

    private ApprovalRule rule(RequestType type, BigDecimal min, BigDecimal max,
                              User approver, int stepOrder, String description) {
        ApprovalRule r = new ApprovalRule();
        r.setRequestType(type);
        r.setMinAmount(min);
        r.setMaxAmount(max);
        r.setApprover(approver);
        r.setStepOrder(stepOrder);
        r.setDescription(description);
        r.setActive(true);
        return r;
    }
}

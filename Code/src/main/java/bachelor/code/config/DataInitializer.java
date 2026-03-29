package bachelor.code.config;

import bachelor.code.entity.User;
import bachelor.code.enums.RoleType;
import bachelor.code.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Creates the first ADMIN user automatically if none exists.
 * Credentials are configured in application.yml under app.admin.*
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.findByEmail(adminEmail).isPresent()) {
            return;
        }

        User admin = new User(adminEmail, "Admin", "Admin", "IT", Set.of(RoleType.ADMIN));
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setPasswordSet(true);
        admin.setActive(true);
        userRepository.save(admin);

        log.info("==============================================");
        log.info("Default admin created:");
        log.info("  Email:    {}", adminEmail);
        log.info("  Password: {}", adminPassword);
        log.info("  URL:      http://localhost:8080/login");
        log.info("==============================================");
    }
}

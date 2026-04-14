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
        seedEmployees();
        seedApprovalRules();
    }

    // ────────────────────────────────────────────
    //  EMPLOYEES — full Medicton staff from thesis
    // ────────────────────────────────────────────

    private void seedEmployees() {
        // ── System admin account (from application.yml) ─
        if (!userRepository.existsByEmail(adminEmail)) {
            User admin = new User(adminEmail, "Admin", "System", "IT",
                    Set.of(RoleType.ADMIN, RoleType.APPROVER, RoleType.ACCOUNTANT));
            admin.setPasswordHash(passwordEncoder.encode(adminPassword));
            admin.setPasswordSet(true);
            admin.setActive(true);
            userRepository.save(admin);
            log.info("  System admin created: {}", adminEmail);
        }

        // ── Management / Vedení ─────────────────────────
        createUser("dobias@medicton.com", "Martin", "Dobiáš",
                "vedení/management", Set.of(RoleType.ADMIN, RoleType.APPROVER, RoleType.REQUESTER));

        createUser("fabian@medicton.com", "Vratislav", "Fabián",
                "vedení/management", Set.of(RoleType.ADMIN, RoleType.APPROVER, RoleType.REQUESTER));

        // ── Management (vedoucí skupin) ─────────────────
        createUser("valentova@medicton.com", "Tereza", "Valentová",
                "management", Set.of(RoleType.APPROVER, RoleType.REQUESTER));  // manažer dispečinku

        createUser("korba@medicton.com", "Matyáš", "Korba",
                "management", Set.of(RoleType.APPROVER, RoleType.REQUESTER));  // servisní manažer

        createUser("vlcek@medicton.com", "Tomáš", "Vlček",
                "management", Set.of(RoleType.APPROVER, RoleType.REQUESTER));  // manažer jakosti a provozu

        createUser("matera@medicton.com", "Lukáš", "Matera",
                "management", Set.of(RoleType.APPROVER, RoleType.REQUESTER));  // manažer nákupu

        createUser("vesela@medicton.com", "Lenka", "Veselá",
                "management", Set.of(RoleType.REQUESTER));  // asistentka vedení

        // ── Účetní / Back office ────────────────────────
        createUser("rohova@medicton.com", "Ivana", "Říhová",
                "management", Set.of(RoleType.ACCOUNTANT, RoleType.REQUESTER));  // finanční manažer, hlavní účetní

        createUser("klimova@medicton.com", "Ivana", "Klímová",
                "Back office", Set.of(RoleType.ACCOUNTANT, RoleType.REQUESTER));  // asistentka fin. oddělení

        // ── Dispečink ───────────────────────────────────
        createUser("cerna@medicton.com", "Alena", "Černá",
                "dispečink", Set.of(RoleType.REQUESTER));

        createUser("klodnerova@medicton.com", "Anežka", "Klodnerová",
                "dispečink", Set.of(RoleType.REQUESTER));

        createUser("scheibova@medicton.com", "Linda", "Scheibová",
                "dispečink", Set.of(RoleType.REQUESTER));

        createUser("buresova@medicton.com", "Lucie", "Burešová",
                "dispečink", Set.of(RoleType.REQUESTER));

        // ── Servis ──────────────────────────────────────
        createUser("pyskaty@medicton.com", "David", "Pyskatý",
                "servis", Set.of(RoleType.REQUESTER));

        createUser("sida@medicton.com", "Jaromír", "Šída",
                "servis", Set.of(RoleType.REQUESTER));

        createUser("masin@medicton.com", "Miroslav", "Mašín",
                "servis", Set.of(RoleType.REQUESTER));

        createUser("botos@medicton.com", "Ondřej", "Botoš",
                "servis", Set.of(RoleType.REQUESTER));

        createUser("pyskaty.p@medicton.com", "Petr", "Pyskatý",
                "servis", Set.of(RoleType.REQUESTER));

        createUser("kalasova@medicton.com", "Lucie", "Kalašová",
                "servis", Set.of(RoleType.REQUESTER));

        createUser("jurencak@medicton.com", "Stanislav", "Juřenčák",
                "servis", Set.of(RoleType.REQUESTER));

        createUser("janis@medicton.com", "Tomáš", "Janiš",
                "servis", Set.of(RoleType.REQUESTER));

        createUser("slezak@medicton.com", "Milan", "Slezák",
                "servis", Set.of(RoleType.REQUESTER));

        // ── Obchod ──────────────────────────────────────
        createUser("miziova@medicton.com", "Lucie", "Miziová",
                "obchod", Set.of(RoleType.REQUESTER));

        createUser("chromcova@medicton.com", "Markéta", "Protivová",
                "obchod", Set.of(RoleType.REQUESTER));

        createUser("jagerova@medicton.com", "Renata", "Jágerová",
                "obchod", Set.of(RoleType.REQUESTER));

        createUser("tajbl@medicton.com", "Václav", "Tajbl",
                "obchod", Set.of(RoleType.REQUESTER));

        createUser("bartovic@medicton.com", "Juraj", "Bartovic",
                "obchod", Set.of(RoleType.REQUESTER));

        // ── Back office (ostatní) ───────────────────────
        createUser("dvorak@medicton.com", "Jiří", "Dvořák",
                "Back office", Set.of(RoleType.REQUESTER));  // skladník a logistik

        createUser("furisova@medicton.com", "Renata", "Furišová",
                "Back office", Set.of(RoleType.REQUESTER));  // asistent servisu

        // ── IT ──────────────────────────────────────────
        createUser("navratil@medicton.com", "Jiří", "Navrátil",
                "IT", Set.of(RoleType.REQUESTER));

        createUser("erlebach@medicton.com", "Jonáš", "Erlebach",
                "IT", Set.of(RoleType.REQUESTER));

        log.info("Employee seed complete — {} users in database.", userRepository.count());
    }

    /**
     * Creates a user only if the email doesn't already exist.
     * New employees get passwordSet=false so they must set their password via email link.
     */
    private User createUser(String email, String firstName, String lastName,
                            String department, Set<RoleType> roles) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            User u = new User(email, firstName, lastName, department, roles);
            // Admin accounts get a pre-set password; regular employees do not
            if (email.equals(adminEmail)) {
                u.setPasswordHash(passwordEncoder.encode(adminPassword));
                u.setPasswordSet(true);
            }
            u.setActive(true);
            User saved = userRepository.save(u);
            log.info("  Created user: {} {} <{}> [{}]", firstName, lastName, email, roles);
            return saved;
        });
    }

    // ────────────────────────────────────────────
    //  APPROVAL RULES — L1 / L2 / L3 + Purchase
    // ────────────────────────────────────────────

    /**
     * Approval rules from thesis:
     *
     * EXPENSE (běžné výdaje):
     *   L1:  0 – 3 000 Kč   → vedoucí skupiny (approverRole = APPROVER — resolved per group)
     *   L2:  3 001 – 50 000  → finanční ředitel (Dobiáš)
     *   L3:  50 001+         → vedení společnosti (Dobiáš + Fabián, 2-step)
     *
     * PURCHASE (nákup zboží):
     *   Step 1: manažer nákupu (Matera)
     *   Step 2: vedení společnosti (Fabián)
     */
    private void seedApprovalRules() {
        if (ruleRepository.count() > 0) {
            return; // already seeded
        }

        User dobias = userRepository.findByEmail("dobias@medicton.com").orElse(null);
        User fabian = userRepository.findByEmail("fabian@medicton.com").orElse(null);
        User matera = userRepository.findByEmail("matera@medicton.com").orElse(null);

        if (dobias == null || fabian == null || matera == null) {
            log.warn("Cannot seed approval rules — key users not found. Seed employees first.");
            return;
        }

        // ── EXPENSE L1: до 3 000 Kč → vedoucí skupiny (role-based) ──
        ruleRepository.save(rule(RequestType.EXPENSE,
                BigDecimal.ZERO, new BigDecimal("3000"),
                null, RoleType.APPROVER, 1,
                "L1: Výdaj do 3 000 Kč → vedoucí skupiny"));

        // ── EXPENSE L2: 3 001 – 50 000 Kč → finanční ředitel (Dobiáš) ──
        ruleRepository.save(rule(RequestType.EXPENSE,
                new BigDecimal("3001"), new BigDecimal("50000"),
                dobias, null, 1,
                "L2: Výdaj 3 001–50 000 Kč → finanční ředitel"));

        // ── EXPENSE L3: nad 50 000 Kč → vedení (Dobiáš step 1, Fabián step 2) ──
        ruleRepository.save(rule(RequestType.EXPENSE,
                new BigDecimal("50001"), null,
                dobias, null, 1,
                "L3: Výdaj nad 50 000 Kč → finanční ředitel (krok 1)"));
        ruleRepository.save(rule(RequestType.EXPENSE,
                new BigDecimal("50001"), null,
                fabian, null, 2,
                "L3: Výdaj nad 50 000 Kč → jednatel (krok 2)"));

        // ── PURCHASE: manažer nákupu → vedení ──
        ruleRepository.save(rule(RequestType.PURCHASE,
                BigDecimal.ZERO, null,
                matera, null, 1,
                "Nákup zboží → manažer nákupu (krok 1)"));
        ruleRepository.save(rule(RequestType.PURCHASE,
                BigDecimal.ZERO, null,
                fabian, null, 2,
                "Nákup zboží → jednatel (krok 2)"));

        log.info("Approval rules seeded: L1 (role-based), L2 (Dobiáš), L3 (Dobiáš+Fabián), PURCHASE (Matera+Fabián)");
    }

    private ApprovalRule rule(RequestType type, BigDecimal min, BigDecimal max,
                              User approver, RoleType approverRole,
                              int stepOrder, String description) {
        ApprovalRule r = new ApprovalRule();
        r.setRequestType(type);
        r.setMinAmount(min);
        r.setMaxAmount(max);
        r.setApprover(approver);
        r.setApproverRole(approverRole);
        r.setStepOrder(stepOrder);
        r.setDescription(description);
        r.setActive(true);
        return r;
    }
}

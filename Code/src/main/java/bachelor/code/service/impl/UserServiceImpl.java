package bachelor.code.service.impl;

import bachelor.code.dto.CreateUserRequest;
import bachelor.code.entity.PasswordSetupToken;
import bachelor.code.entity.User;
import bachelor.code.exception.BusinessRuleViolationException;
import bachelor.code.exception.ResourceNotFoundException;
import bachelor.code.repository.UserRepository;
import bachelor.code.service.EmailService;
import bachelor.code.service.PasswordSetupService;
import bachelor.code.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordSetupService passwordSetupService;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository,
                           EmailService emailService,
                           PasswordSetupService passwordSetupService,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.passwordSetupService = passwordSetupService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public User createUser(CreateUserRequest dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new BusinessRuleViolationException("User with email '" + dto.getEmail() + "' already exists");
        }

        User user = new User(dto.getEmail(), dto.getFirstName(), dto.getLastName(), dto.getDepartment(), dto.getRoles());
        User saved = userRepository.save(user);

        PasswordSetupToken token = passwordSetupService.generateTokenForUser(saved);
        emailService.sendPasswordSetupEmail(saved.getEmail(), token.getToken());

        return saved;
    }

    @Override
    @Transactional
    public void resendSetupLink(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        PasswordSetupToken token = passwordSetupService.generateTokenForUser(user);
        emailService.sendPasswordSetupEmail(user.getEmail(), token.getToken());
    }

    @Override
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    @Override
    public boolean authenticate(String email, String rawPassword) {
        return userRepository.findByEmail(email)
                .map(u -> u.isActive() && u.isPasswordSet() && passwordEncoder.matches(rawPassword, u.getPasswordHash()))
                .orElse(false);
    }
}

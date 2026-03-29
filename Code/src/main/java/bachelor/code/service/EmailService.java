package bachelor.code.service;

public interface EmailService {
    void sendPasswordSetupEmail(String to, String token);
}

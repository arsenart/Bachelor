package bachelor.code.service.impl;

import bachelor.code.service.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final String appBaseUrl;

    public EmailServiceImpl(JavaMailSender mailSender, @Value("${app.base-url}") String appBaseUrl) {
        this.mailSender = mailSender;
        this.appBaseUrl = appBaseUrl;
    }

    @Override
    public void sendPasswordSetupEmail(String to, String token) {
        String link = appBaseUrl + "/setup-password?token=" + token;
        String subject = "[MyApp] Set up your account password";
        String text = "Hello,\n\n" +
                "An account has been created for you. Please click the link below to set your password (valid for 24 hours):\n\n" +
                link +
                "\n\nIf you did not expect this email, please contact your administrator.";

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(text);
        mailSender.send(msg);
    }
}

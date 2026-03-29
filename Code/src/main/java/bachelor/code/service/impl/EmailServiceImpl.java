package bachelor.code.service.impl;

import bachelor.code.service.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final String appBaseUrl;

    public EmailServiceImpl(JavaMailSender mailSender,
                            @Value("${app.base-url}") String appBaseUrl) {
        this.mailSender = mailSender;
        this.appBaseUrl = appBaseUrl;
    }

    @Override
    public void sendPasswordSetupEmail(String to, String token) {
        String link = appBaseUrl + "/setup-password?token=" + token;
        send(to,
             "[MyApp] Set up your account password",
             "Hello,\n\n"
             + "An account has been created for you.\n"
             + "Please click the link below to set your password (valid for 24 hours):\n\n"
             + link
             + "\n\nIf you did not expect this email, please contact your administrator.");
    }

    @Override
    public void sendApprovalNeededEmail(String to, String requesterName,
                                        String requestTitle, BigDecimal amount, String currency) {
        String link = appBaseUrl + "/approvals";
        send(to,
             "[MyApp] Approval required: " + requestTitle,
             "Hello,\n\n"
             + requesterName + " has submitted a request requiring your approval:\n\n"
             + "  Title:  " + requestTitle + "\n"
             + "  Amount: " + amount + " " + currency + "\n\n"
             + "Please review it here:\n" + link
             + "\n\nThis is an automated notification.");
    }

    @Override
    public void sendRequestDecisionEmail(String to, String requestTitle,
                                         String decision, String comment) {
        String link = appBaseUrl + "/requests";
        String commentLine = (comment != null && !comment.isBlank())
                ? "\n  Comment: " + comment
                : "";
        send(to,
             "[MyApp] Request update: " + requestTitle,
             "Hello,\n\n"
             + "Your request \"" + requestTitle + "\" has been " + decision + "." + commentLine + "\n\n"
             + "View your requests here:\n" + link
             + "\n\nThis is an automated notification.");
    }

    private void send(String to, String subject, String text) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(text);
        mailSender.send(msg);
    }
}

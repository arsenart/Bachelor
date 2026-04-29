package bachelor.code.service.impl;

import bachelor.code.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);
    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

    private final RestTemplate restTemplate = new RestTemplate();
    private final MessageSource messageSource;
    private final String appBaseUrl;
    private final String mailFrom;
    private final String brevoApiKey;

    public EmailServiceImpl(MessageSource messageSource,
                            @Value("${app.base-url}") String appBaseUrl,
                            @Value("${app.mail.from:noreply@medicton.com}") String mailFrom,
                            @Value("${app.mail.brevo-api-key:}") String brevoApiKey) {
        this.messageSource = messageSource;
        this.appBaseUrl = appBaseUrl;
        this.mailFrom = mailFrom;
        this.brevoApiKey = brevoApiKey;
    }

    @Override
    public void sendPasswordSetupEmail(String to, String token) {
        String link = appBaseUrl + "/setup-password?token=" + token;
        send(to,
             subject("email.subject.password_setup"),
             "Dobrý den,\n\n"
             + "byl pro Vás vytvořen účet v systému správy nákupů.\n"
             + "Pro nastavení hesla klikněte na následující odkaz (platnost 24 hodin):\n\n"
             + link
             + "\n\nPokud jste tento e-mail neočekávali, obraťte se na administrátora.");
    }

    @Override
    public void sendApprovalNeededEmail(String to, String requesterName,
                                        String requestTitle, BigDecimal amount, String currency) {
        String link = appBaseUrl + "/approvals";
        send(to,
             subject("email.subject.approval_needed") + ": " + requestTitle,
             "Dobrý den,\n\n"
             + requesterName + " odeslal/a žádost, která vyžaduje Vaše schválení:\n\n"
             + "  Název:  " + requestTitle + "\n"
             + "  Částka: " + amount + " " + currency + "\n\n"
             + "Zkontrolovat ji můžete zde:\n" + link
             + "\n\nToto je automatické upozornění.");
    }

    @Override
    public void sendRequestDecisionEmail(String to, String requestTitle,
                                         String decision, String comment) {
        String link = appBaseUrl + "/requests";
        String commentLine = (comment != null && !comment.isBlank())
                ? "\n  Komentář: " + comment
                : "";
        send(to,
             subject("email.subject.request_decision") + ": " + requestTitle,
             "Dobrý den,\n\n"
             + "Vaše žádost \"" + requestTitle + "\" byla: " + decision + "." + commentLine + "\n\n"
             + "Své žádosti zobrazíte zde:\n" + link
             + "\n\nToto je automatické upozornění.");
    }

    @Override
    public void sendDocumentStatusEmail(String to, String documentInfo,
                                        String newStatus, String comment) {
        String link = appBaseUrl + "/accounting";
        String commentLine = (comment != null && !comment.isBlank())
                ? "\n  Komentář: " + comment
                : "";
        send(to,
             "Aktualizace dokladu: " + documentInfo,
             "Dobrý den,\n\n"
             + "Stav účetního dokladu \"" + documentInfo + "\" byl změněn na: " + newStatus + "." + commentLine + "\n\n"
             + "Detail zobrazíte zde:\n" + link
             + "\n\nToto je automatické upozornění.");
    }

    private String subject(String key) {
        Locale locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(key, null, key, locale);
    }

    private void send(String to, String subject, String text) {
        if (brevoApiKey == null || brevoApiKey.isBlank()) {
            log.warn("BREVO_API_KEY not configured — skipping email to {}", to);
            return;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", brevoApiKey);

            Map<String, Object> body = Map.of(
                    "sender", Map.of("name", "Systém správy nákupů", "email", mailFrom),
                    "to", List.of(Map.of("email", to)),
                    "subject", subject,
                    "textContent", text
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            restTemplate.postForEntity(BREVO_API_URL, request, String.class);
            log.info("Email sent to {} | subject: {}", to, subject);
        } catch (Exception e) {
            log.error("Failed to send email to {} | subject: {} | error: {}", to, subject, e.getMessage());
        }
    }
}

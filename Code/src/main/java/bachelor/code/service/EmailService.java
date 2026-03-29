package bachelor.code.service;

import java.math.BigDecimal;

public interface EmailService {

    void sendPasswordSetupEmail(String to, String token);

    // Notify approver that a new request requires their decision
    void sendApprovalNeededEmail(String to, String requesterName,
                                 String requestTitle, BigDecimal amount, String currency);

    // Notify requester about the decision on their request
    void sendRequestDecisionEmail(String to, String requestTitle,
                                  String decision, String comment);
}

package bachelor.code.repository;

import bachelor.code.entity.AccountingDocument;
import bachelor.code.entity.User;
import bachelor.code.enums.DocumentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountingDocumentRepository extends JpaRepository<AccountingDocument, Long> {

    List<AccountingDocument> findBySubmittedByOrderByCreatedAtDesc(User submittedBy);

    List<AccountingDocument> findByStatusOrderByCreatedAtDesc(DocumentStatus status);

    List<AccountingDocument> findByStatusInOrderByCreatedAtDesc(List<DocumentStatus> statuses);

    List<AccountingDocument> findAllByOrderByCreatedAtDesc();

    long countBySubmittedByAndStatus(User submittedBy, DocumentStatus status);

    long countByStatus(DocumentStatus status);

    List<AccountingDocument> findByApprovalRequestId(Long approvalRequestId);
}

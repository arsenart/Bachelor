package bachelor.code.service;

import bachelor.code.dto.CreateAccountingDocumentDto;
import bachelor.code.entity.AccountingDocument;
import bachelor.code.entity.User;
import bachelor.code.enums.DocumentStatus;

import java.util.List;

public interface AccountingDocumentService {

    AccountingDocument create(CreateAccountingDocumentDto dto, User submitter);

    AccountingDocument update(Long id, CreateAccountingDocumentDto dto, User submitter);

    void submitToAccounting(Long id, User submitter);

    void returnForCompletion(Long id, User accountant, String comment);

    void markAsPosted(Long id, User accountant, String pohodaNumber);

    void markAsPaid(Long id, User accountant);

    void close(Long id, User user);

    AccountingDocument getById(Long id);

    List<AccountingDocument> getBySubmitter(User submitter);

    List<AccountingDocument> getPendingForAccountant();

    List<AccountingDocument> getAll();

    long countBySubmitterAndStatus(User submitter, DocumentStatus status);

    long countPendingForAccountant();
}

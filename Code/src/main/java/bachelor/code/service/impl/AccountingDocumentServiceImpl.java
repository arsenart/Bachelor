package bachelor.code.service.impl;

import bachelor.code.dto.CreateAccountingDocumentDto;
import bachelor.code.entity.AccountingDocument;
import bachelor.code.entity.ApprovalRequest;
import bachelor.code.entity.User;
import bachelor.code.enums.DocumentStatus;
import bachelor.code.exception.BusinessRuleViolationException;
import bachelor.code.exception.ResourceNotFoundException;
import bachelor.code.enums.RoleType;
import bachelor.code.repository.AccountingDocumentRepository;
import bachelor.code.repository.ApprovalRequestRepository;
import bachelor.code.repository.UserRepository;
import bachelor.code.service.AccountingDocumentService;
import bachelor.code.service.AuditLogService;
import bachelor.code.service.EmailService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AccountingDocumentServiceImpl implements AccountingDocumentService {

    private final AccountingDocumentRepository documentRepository;
    private final ApprovalRequestRepository requestRepository;
    private final AuditLogService auditLogService;
    private final EmailService emailService;
    private final UserRepository userRepository;

    public AccountingDocumentServiceImpl(AccountingDocumentRepository documentRepository,
                                         ApprovalRequestRepository requestRepository,
                                         AuditLogService auditLogService,
                                         EmailService emailService,
                                         UserRepository userRepository) {
        this.documentRepository = documentRepository;
        this.requestRepository = requestRepository;
        this.auditLogService = auditLogService;
        this.emailService = emailService;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public AccountingDocument create(CreateAccountingDocumentDto dto, User submitter) {
        AccountingDocument doc = new AccountingDocument();
        applyDto(doc, dto);
        doc.setSubmittedBy(submitter);
        doc.setStatus(DocumentStatus.NEW);

        if (dto.getApprovalRequestId() != null) {
            ApprovalRequest request = requestRepository.findById(dto.getApprovalRequestId())
                    .orElseThrow(() -> new ResourceNotFoundException("Request #" + dto.getApprovalRequestId() + " not found"));
            doc.setApprovalRequest(request);
        }

        AccountingDocument saved = documentRepository.save(doc);
        auditLogService.log("AccountingDocument", saved.getId(), "CREATED",
                submitter, null, DocumentStatus.NEW.name(), null);
        return saved;
    }

    @Override
    @Transactional
    public AccountingDocument update(Long id, CreateAccountingDocumentDto dto, User submitter) {
        AccountingDocument doc = getById(id);
        if (!doc.isEditable()) {
            throw new BusinessRuleViolationException("Document #" + id + " cannot be edited in status: " + doc.getStatus());
        }
        if (!doc.getSubmittedBy().getId().equals(submitter.getId())) {
            throw new BusinessRuleViolationException("You can only edit your own documents.");
        }

        applyDto(doc, dto);
        AccountingDocument saved = documentRepository.save(doc);
        auditLogService.log("AccountingDocument", id, "UPDATED",
                submitter, null, null, null);
        return saved;
    }

    @Override
    @Transactional
    public void submitToAccounting(Long id, User submitter) {
        AccountingDocument doc = getById(id);
        if (!doc.isSubmittable()) {
            throw new BusinessRuleViolationException("Document #" + id + " cannot be submitted in status: " + doc.getStatus());
        }
        if (!doc.getSubmittedBy().getId().equals(submitter.getId())) {
            throw new BusinessRuleViolationException("You can only submit your own documents.");
        }

        String oldStatus = doc.getStatus().name();
        doc.setStatus(DocumentStatus.SUBMITTED_TO_ACCOUNTING);
        documentRepository.save(doc);
        auditLogService.log("AccountingDocument", id, "SUBMITTED_TO_ACCOUNTING",
                submitter, oldStatus, DocumentStatus.SUBMITTED_TO_ACCOUNTING.name(), null);

        String docInfo = doc.getType() + " #" + doc.getId() + " — " + doc.getSupplierName();
        userRepository.findByRolesContainingAndActiveTrue(RoleType.ACCOUNTANT)
                .forEach(accountant -> emailService.sendDocumentStatusEmail(
                        accountant.getEmail(), docInfo, "SUBMITTED_TO_ACCOUNTING", null));
    }

    @Override
    @Transactional
    public void returnForCompletion(Long id, User accountant, String comment) {
        AccountingDocument doc = getById(id);
        if (doc.getStatus() != DocumentStatus.SUBMITTED_TO_ACCOUNTING) {
            throw new BusinessRuleViolationException("Document #" + id + " is not pending accounting review.");
        }

        doc.setStatus(DocumentStatus.RETURNED_FOR_COMPLETION);
        documentRepository.save(doc);
        auditLogService.log("AccountingDocument", id, "RETURNED_FOR_COMPLETION",
                accountant, DocumentStatus.SUBMITTED_TO_ACCOUNTING.name(),
                DocumentStatus.RETURNED_FOR_COMPLETION.name(), comment);

        String docInfo = doc.getType() + " #" + doc.getId() + " — " + doc.getSupplierName();
        emailService.sendDocumentStatusEmail(
                doc.getSubmittedBy().getEmail(), docInfo, "RETURNED_FOR_COMPLETION", comment);
    }

    @Override
    @Transactional
    public void markAsPosted(Long id, User accountant, String pohodaNumber) {
        AccountingDocument doc = getById(id);
        if (doc.getStatus() != DocumentStatus.SUBMITTED_TO_ACCOUNTING) {
            throw new BusinessRuleViolationException("Document #" + id + " is not pending accounting review.");
        }

        doc.setStatus(DocumentStatus.POSTED);
        doc.setProcessedBy(accountant);
        if (pohodaNumber != null && !pohodaNumber.isBlank()) {
            doc.setPohodaNumber(pohodaNumber);
        }
        documentRepository.save(doc);
        auditLogService.log("AccountingDocument", id, "POSTED",
                accountant, DocumentStatus.SUBMITTED_TO_ACCOUNTING.name(),
                DocumentStatus.POSTED.name(), pohodaNumber);

        String docInfo = doc.getType() + " #" + doc.getId() + " — " + doc.getSupplierName();
        emailService.sendDocumentStatusEmail(
                doc.getSubmittedBy().getEmail(), docInfo, "POSTED", null);
    }

    @Override
    @Transactional
    public void markAsPaid(Long id, User accountant) {
        AccountingDocument doc = getById(id);
        if (doc.getStatus() != DocumentStatus.POSTED) {
            throw new BusinessRuleViolationException("Document #" + id + " must be posted before marking as paid.");
        }

        doc.setStatus(DocumentStatus.PAID);
        doc.setPaidAt(LocalDateTime.now());
        documentRepository.save(doc);
        auditLogService.log("AccountingDocument", id, "PAID",
                accountant, DocumentStatus.POSTED.name(), DocumentStatus.PAID.name(), null);

        String docInfo = doc.getType() + " #" + doc.getId() + " — " + doc.getSupplierName();
        emailService.sendDocumentStatusEmail(
                doc.getSubmittedBy().getEmail(), docInfo, "PAID", null);
    }

    @Override
    @Transactional
    public void close(Long id, User user) {
        AccountingDocument doc = getById(id);
        if (doc.getStatus() != DocumentStatus.PAID && doc.getStatus() != DocumentStatus.POSTED) {
            throw new BusinessRuleViolationException("Document #" + id + " must be posted or paid before closing.");
        }

        String oldStatus = doc.getStatus().name();
        doc.setStatus(DocumentStatus.CLOSED);
        documentRepository.save(doc);
        auditLogService.log("AccountingDocument", id, "CLOSED",
                user, oldStatus, DocumentStatus.CLOSED.name(), null);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountingDocument getById(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document #" + id + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountingDocument> getBySubmitter(User submitter) {
        return documentRepository.findBySubmittedByOrderByCreatedAtDesc(submitter);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountingDocument> getPendingForAccountant() {
        return documentRepository.findByStatusOrderByCreatedAtDesc(DocumentStatus.SUBMITTED_TO_ACCOUNTING);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountingDocument> getAll() {
        return documentRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public long countBySubmitterAndStatus(User submitter, DocumentStatus status) {
        return documentRepository.countBySubmittedByAndStatus(submitter, status);
    }

    @Override
    @Transactional(readOnly = true)
    public long countPendingForAccountant() {
        return documentRepository.countByStatus(DocumentStatus.SUBMITTED_TO_ACCOUNTING);
    }

    private void applyDto(AccountingDocument doc, CreateAccountingDocumentDto dto) {
        doc.setType(dto.getType());
        doc.setAmountWithoutVat(dto.getAmountWithoutVat());
        doc.setAmountWithVat(dto.getAmountWithVat());
        doc.setSupplierIco(dto.getSupplierIco());
        doc.setSupplierName(dto.getSupplierName());
        doc.setDescription(dto.getDescription());
        doc.setJustification(dto.getJustification());
        doc.setDepartment(dto.getDepartment());
        doc.setTaxDate(dto.getTaxDate());
        doc.setDocumentLocation(dto.getDocumentLocation());
    }
}

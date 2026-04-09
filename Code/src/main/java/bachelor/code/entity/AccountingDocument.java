package bachelor.code.entity;

import bachelor.code.enums.DocumentStatus;
import bachelor.code.enums.DocumentType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "accounting_documents")
public class AccountingDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentType type;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amountWithoutVat;

    @Column(precision = 15, scale = 2)
    private BigDecimal amountWithVat;

    @Column(length = 20)
    private String supplierIco;

    @Column(length = 255)
    private String supplierName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String justification;

    @Column(length = 100)
    private String department;

    private LocalDate taxDate;

    @Column(length = 500)
    private String documentLocation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentStatus status = DocumentStatus.NEW;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by_id", nullable = false)
    private User submittedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approval_request_id")
    private ApprovalRequest approvalRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by_id")
    private User processedBy;

    @Column(length = 50)
    private String pohodaNumber;

    private LocalDate dueDate;

    private LocalDateTime paidAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public AccountingDocument() {}

    // Getters and setters

    public Long getId() { return id; }

    public DocumentType getType() { return type; }
    public void setType(DocumentType type) { this.type = type; }

    public BigDecimal getAmountWithoutVat() { return amountWithoutVat; }
    public void setAmountWithoutVat(BigDecimal amountWithoutVat) { this.amountWithoutVat = amountWithoutVat; }

    public BigDecimal getAmountWithVat() { return amountWithVat; }
    public void setAmountWithVat(BigDecimal amountWithVat) { this.amountWithVat = amountWithVat; }

    public String getSupplierIco() { return supplierIco; }
    public void setSupplierIco(String supplierIco) { this.supplierIco = supplierIco; }

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getJustification() { return justification; }
    public void setJustification(String justification) { this.justification = justification; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public LocalDate getTaxDate() { return taxDate; }
    public void setTaxDate(LocalDate taxDate) { this.taxDate = taxDate; }

    public String getDocumentLocation() { return documentLocation; }
    public void setDocumentLocation(String documentLocation) { this.documentLocation = documentLocation; }

    public DocumentStatus getStatus() { return status; }
    public void setStatus(DocumentStatus status) { this.status = status; }

    public User getSubmittedBy() { return submittedBy; }
    public void setSubmittedBy(User submittedBy) { this.submittedBy = submittedBy; }

    public ApprovalRequest getApprovalRequest() { return approvalRequest; }
    public void setApprovalRequest(ApprovalRequest approvalRequest) { this.approvalRequest = approvalRequest; }

    public User getProcessedBy() { return processedBy; }
    public void setProcessedBy(User processedBy) { this.processedBy = processedBy; }

    public String getPohodaNumber() { return pohodaNumber; }
    public void setPohodaNumber(String pohodaNumber) { this.pohodaNumber = pohodaNumber; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public boolean isEditable() {
        return status == DocumentStatus.NEW || status == DocumentStatus.RETURNED_FOR_COMPLETION;
    }

    public boolean isSubmittable() {
        return status == DocumentStatus.NEW || status == DocumentStatus.RETURNED_FOR_COMPLETION;
    }
}

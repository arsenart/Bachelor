package bachelor.code.dto;

import bachelor.code.enums.DocumentType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CreateAccountingDocumentDto {

    @NotNull(message = "Document type is required")
    private DocumentType type;

    @NotNull(message = "Amount without VAT is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amountWithoutVat;

    private BigDecimal amountWithVat;

    private String supplierIco;

    @NotEmpty(message = "Supplier name is required")
    private String supplierName;

    private String description;

    private String justification;

    private String department;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate taxDate;

    private String documentLocation;

    private Long approvalRequestId;

    // Getters and setters

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

    public Long getApprovalRequestId() { return approvalRequestId; }
    public void setApprovalRequestId(Long approvalRequestId) { this.approvalRequestId = approvalRequestId; }
}

package bachelor.code.dto;

import bachelor.code.enums.RequestType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CreateApprovalRequestDto {

    @NotEmpty(message = "Title is required")
    private String title;

    @NotNull(message = "Request type is required")
    private RequestType type;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    private String currency = "CZK";

    private String supplier;

    private String description;

    private String justification;

    private String department;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate requestedDate;

    // URL to an external document (SharePoint, Drive, etc.)
    private String documentLink;

    // Getters and setters

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public RequestType getType() { return type; }
    public void setType(RequestType type) { this.type = type; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getSupplier() { return supplier; }
    public void setSupplier(String supplier) { this.supplier = supplier; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getJustification() { return justification; }
    public void setJustification(String justification) { this.justification = justification; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public LocalDate getRequestedDate() { return requestedDate; }
    public void setRequestedDate(LocalDate requestedDate) { this.requestedDate = requestedDate; }

    public String getDocumentLink() { return documentLink; }
    public void setDocumentLink(String documentLink) { this.documentLink = documentLink; }
}

package bachelor.code.dto;

public class ApprovalDecisionDto {

    // Comment is required for REJECT and RETURN, optional for APPROVE
    private String comment;

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}

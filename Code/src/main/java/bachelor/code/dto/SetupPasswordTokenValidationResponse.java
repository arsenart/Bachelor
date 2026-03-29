package bachelor.code.dto;

public class SetupPasswordTokenValidationResponse {
    private boolean valid;
    private String reason; // null or EXPIRED / USED / NOT_FOUND

    public SetupPasswordTokenValidationResponse() {}

    public SetupPasswordTokenValidationResponse(boolean valid, String reason) {
        this.valid = valid;
        this.reason = reason;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}

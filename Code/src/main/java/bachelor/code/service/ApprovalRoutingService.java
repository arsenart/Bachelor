package bachelor.code.service;

import bachelor.code.entity.ApprovalRequest;
import bachelor.code.entity.ApprovalStep;

import java.util.List;

public interface ApprovalRoutingService {

    // Determines the ordered list of steps based on ApprovalRule configuration
    List<ApprovalStep> buildStepsForRequest(ApprovalRequest request);
}

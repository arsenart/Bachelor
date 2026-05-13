package bachelor.code.repository;

import bachelor.code.entity.ApprovalStep;
import bachelor.code.entity.User;
import bachelor.code.enums.StepStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApprovalStepRepository extends JpaRepository<ApprovalStep, Long> {

    List<ApprovalStep> findByApproverAndStatus(User approver, StepStatus status);

    // All requests where this user has a pending step — used for approver's inbox
    @Query("SELECT DISTINCT s.request FROM ApprovalStep s " +
           "WHERE s.approver = :approver AND s.status = :status " +
           "ORDER BY s.request.createdAt DESC")
    List<bachelor.code.entity.ApprovalRequest> findRequestsByApproverAndStatus(
            @Param("approver") User approver,
            @Param("status") StepStatus status);

    long countByApproverAndStatus(User approver, StepStatus status);

    // Count DISTINCT requests where this user has a pending step — used for dashboard card
    @Query("SELECT COUNT(DISTINCT s.request) FROM ApprovalStep s " +
           "WHERE s.approver = :approver AND s.status = :status")
    long countDistinctRequestsByApproverAndStatus(
            @Param("approver") User approver,
            @Param("status") StepStatus status);
}

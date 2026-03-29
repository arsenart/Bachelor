package bachelor.code.repository;

import bachelor.code.entity.ApprovalRequest;
import bachelor.code.entity.User;
import bachelor.code.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, Long> {

    List<ApprovalRequest> findByRequestedByOrderByCreatedAtDesc(User requestedBy);

    List<ApprovalRequest> findByStatusOrderByCreatedAtDesc(RequestStatus status);

    List<ApprovalRequest> findAllByOrderByCreatedAtDesc();

    // Fetch request with steps and approvers in a single query to avoid N+1
    @Query("SELECT DISTINCT r FROM ApprovalRequest r " +
           "LEFT JOIN FETCH r.requestedBy " +
           "LEFT JOIN FETCH r.steps s " +
           "LEFT JOIN FETCH s.approver " +
           "WHERE r.id = :id")
    Optional<ApprovalRequest> findByIdWithDetails(@Param("id") Long id);

    // Count active requests for dashboard stats
    long countByRequestedByAndStatus(User requestedBy, RequestStatus status);
}

package bachelor.code.repository;

import bachelor.code.entity.ApprovalRule;
import bachelor.code.enums.RequestType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApprovalRuleRepository extends JpaRepository<ApprovalRule, Long> {

    // All active rules for a request type, ordered by step (used by routing service)
    List<ApprovalRule> findByRequestTypeAndActiveTrueOrderByStepOrderAsc(RequestType requestType);

    List<ApprovalRule> findAllByOrderByRequestTypeAscStepOrderAsc();
}

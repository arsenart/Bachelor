package bachelor.code.repository;

import bachelor.code.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    // Used by ApprovalRoutingService for role-based routing
    Optional<User> findFirstByRolesContainingAndActiveTrue(bachelor.code.enums.RoleType role);

    Optional<User> findFirstByRolesContainingAndActiveTrueAndIdNot(bachelor.code.enums.RoleType role, Long excludeId);

    java.util.List<User> findByRolesContainingAndActiveTrue(bachelor.code.enums.RoleType role);
}

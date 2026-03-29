package bachelor.code.dto;

import bachelor.code.entity.User;
import bachelor.code.enums.RoleType;

import java.util.Set;

public class UserResponse {

    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String department;
    private boolean active;
    private boolean passwordSet;
    private Set<RoleType> roles;

    public static UserResponse from(User user) {
        UserResponse r = new UserResponse();
        r.id = user.getId();
        r.email = user.getEmail();
        r.firstName = user.getFirstName();
        r.lastName = user.getLastName();
        r.department = user.getDepartment();
        r.active = user.isActive();
        r.passwordSet = user.isPasswordSet();
        r.roles = user.getRoles();
        return r;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getDepartment() { return department; }
    public boolean isActive() { return active; }
    public boolean isPasswordSet() { return passwordSet; }
    public Set<RoleType> getRoles() { return roles; }
}

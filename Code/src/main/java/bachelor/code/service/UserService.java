package bachelor.code.service;

import bachelor.code.dto.CreateUserRequest;
import bachelor.code.entity.User;

public interface UserService {
    User createUser(CreateUserRequest dto);
    void resendSetupLink(Long userId);
    boolean authenticate(String email, String rawPassword);
}

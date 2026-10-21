package com.lawfirm.lcm.service;

import com.lawfirm.lcm.domain.User;
import com.lawfirm.lcm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public User createUser(String email, String fullName, User.Role role, String rawPassword, User actor) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Email already in use");
        }
        User u = new User();
        u.setEmail(email);
        u.setFullName(fullName);
        u.setRole(role);
        u.setPasswordHash(passwordEncoder.encode(rawPassword));
        u = userRepository.save(u);
        auditService.log(actor, "USER_CREATE", "User", u.getId(), null, role.name());
        return u;
    }

    public void changeRole(User target, User.Role newRole, User actor) {
        String before = target.getRole().name();
        target.setRole(newRole);
        userRepository.save(target);
        auditService.log(actor, "USER_ROLE_CHANGE", "User", target.getId(), before, newRole.name());
    }

    public void setEnabled(User target, boolean enabled, User actor) {
        String before = String.valueOf(target.isEnabled());
        target.setEnabled(enabled);
        userRepository.save(target);
        auditService.log(actor, enabled ? "USER_ENABLE" : "USER_DISABLE", "User", target.getId(), before, String.valueOf(enabled));
    }

    public void changePassword(User user, String rawPassword) {
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        userRepository.save(user);
    }
}

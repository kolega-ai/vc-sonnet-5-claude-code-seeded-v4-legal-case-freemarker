package com.lawfirm.lcm.service;

import com.lawfirm.lcm.domain.PasswordResetToken;
import com.lawfirm.lcm.domain.User;
import com.lawfirm.lcm.repository.PasswordResetTokenRepository;
import com.lawfirm.lcm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final OutboxService outboxService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public boolean requestReset(String email) {
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email);
        userOpt.ifPresent(user -> {
            byte[] bytes = new byte[32];
            secureRandom.nextBytes(bytes);
            String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            PasswordResetToken prt = new PasswordResetToken();
            prt.setUser(user);
            prt.setToken(token);
            prt.setExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
            tokenRepository.save(prt);
            outboxService.send(user.getEmail(), "Password reset request",
                    "Use this link to reset your password: /reset-password?token=" + token + " (valid 1 hour, single use).",
                    "PASSWORD_RESET");
        });
        return userOpt.isPresent();
    }

    public boolean resetPassword(String token, String newPassword) {
        Optional<PasswordResetToken> prtOpt = tokenRepository.findByToken(token);
        if (prtOpt.isEmpty()) return false;
        PasswordResetToken prt = prtOpt.get();
        if (prt.isUsed() || prt.getExpiresAt().isBefore(Instant.now())) {
            return false;
        }
        User user = prt.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        prt.setUsed(true);
        tokenRepository.save(prt);
        return true;
    }
}

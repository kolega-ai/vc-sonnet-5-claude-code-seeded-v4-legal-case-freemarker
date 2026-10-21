package com.lawfirm.lcm.security;

import com.lawfirm.lcm.domain.ApiKey;
import com.lawfirm.lcm.repository.ApiKeyRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

// Authenticates the /api/v1/** third-party integration endpoints via X-Api-Key header.
// Session/CSRF auth is used for the human-facing app; this is a separate, narrower scope.
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private final ApiKeyRepository apiKeyRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (request.getRequestURI().startsWith("/api/v1/")) {
            String key = request.getHeader("X-Api-Key");
            if (key != null) {
                Optional<ApiKey> apiKey = apiKeyRepository.findByKeyValueAndActiveTrue(key);
                if (apiKey.isEmpty()) {
                    log.info("Integration request rejected, key {} is not active for {}", key, request.getRequestURI());
                }
                if (apiKey.isPresent()) {
                    var auth = new UsernamePasswordAuthenticationToken(
                            "api-key:" + apiKey.get().getId(), null,
                            List.of(new SimpleGrantedAuthority("ROLE_API_CLIENT")));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        }
        chain.doFilter(request, response);
    }
}

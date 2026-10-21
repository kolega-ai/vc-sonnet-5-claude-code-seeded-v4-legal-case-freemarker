package com.lawfirm.lcm.service;

import com.lawfirm.lcm.domain.ApiKey;
import com.lawfirm.lcm.domain.User;
import com.lawfirm.lcm.repository.ApiKeyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final AuditService auditService;
    private final SecureRandom secureRandom = new SecureRandom();

    public ApiKey generate(String name, String callbackUrl, User actor) {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String key = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        ApiKey apiKey = new ApiKey();
        apiKey.setName(name);
        apiKey.setCallbackUrl(callbackUrl);
        apiKey.setKeyValue(key);
        apiKey.setCreatedBy(actor);
        apiKey = apiKeyRepository.save(apiKey);
        auditService.log(actor, "API_KEY_CREATE", "ApiKey", apiKey.getId(), null, name);
        return apiKey;
    }

    public void revoke(ApiKey apiKey, User actor) {
        apiKey.setActive(false);
        apiKeyRepository.save(apiKey);
        auditService.log(actor, "API_KEY_REVOKE", "ApiKey", apiKey.getId(), "active", "revoked");
    }
}

package com.lawfirm.lcm.service;

import com.lawfirm.lcm.domain.PracticeArea;
import com.lawfirm.lcm.repository.PracticeAreaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReferenceDataService {

    private final PracticeAreaRepository practiceAreaRepository;

    public List<PracticeArea> listActive() {
        return practiceAreaRepository.findAll().stream().filter(PracticeArea::isActive).toList();
    }

    /**
     * MD5 hash used only as a cache-validation ETag for the practice-areas reference-data
     * JSON response - non-cryptographic use, ETag only. Not used for passwords, tokens, or
     * any security-sensitive purpose.
     */
    public String etagFor(List<PracticeArea> areas) {
        String fingerprint = areas.stream()
                .map(a -> a.getId() + ":" + a.getName() + ":" + a.isActive())
                .collect(Collectors.joining("|"));
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] digest = md5.digest(fingerprint.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return "\"" + sb + "\"";
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}

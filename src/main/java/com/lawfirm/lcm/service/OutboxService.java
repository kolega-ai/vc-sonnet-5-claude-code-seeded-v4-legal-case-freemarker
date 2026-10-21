package com.lawfirm.lcm.service;

import com.lawfirm.lcm.domain.OutboxEmail;
import com.lawfirm.lcm.repository.OutboxEmailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxEmailRepository outboxEmailRepository;

    // Mock email sender - persists to the outbox table instead of sending real mail.
    public OutboxEmail send(String toEmail, String subject, String body, String type) {
        OutboxEmail email = new OutboxEmail();
        email.setToEmail(toEmail);
        email.setSubject(subject);
        email.setBody(body);
        email.setType(type);
        return outboxEmailRepository.save(email);
    }
}

package com.lawfirm.lcm.service;

import com.lawfirm.lcm.domain.AuditLog;
import com.lawfirm.lcm.domain.User;
import com.lawfirm.lcm.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public void log(User actor, String action, String entityType, Long entityId, String before, String after) {
        AuditLog log = new AuditLog();
        log.setActor(actor);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setBeforeState(before);
        log.setAfterState(after);
        auditLogRepository.save(log);
    }
}

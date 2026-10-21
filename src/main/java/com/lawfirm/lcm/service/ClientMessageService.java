package com.lawfirm.lcm.service;

import com.lawfirm.lcm.domain.ClientMessage;
import com.lawfirm.lcm.domain.User;
import com.lawfirm.lcm.repository.ClientMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClientMessageService {

    private final ClientMessageRepository clientMessageRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;

    public void approveOrSend(ClientMessage message, ClientMessage.Status target, User actor) {
        if (actor.getRole() != User.Role.ATTORNEY && actor.getRole() != User.Role.PARALEGAL && actor.getRole() != User.Role.FIRM_ADMIN) {
            throw new AccessDeniedException("Only staff may approve or send client messages");
        }
        if (message.getStatus() != ClientMessage.Status.DRAFT) {
            throw new IllegalStateException("Only draft messages can be approved/sent");
        }
        message.setStatus(target);
        message.setApprovedBy(actor);
        clientMessageRepository.save(message);
        auditService.log(actor, "MESSAGE_" + target.name(), "ClientMessage", message.getId(), "DRAFT", target.name());
    }

    public boolean clientCanView(ClientMessage message) {
        return message.getStatus() == ClientMessage.Status.APPROVED || message.getStatus() == ClientMessage.Status.SENT;
    }
}

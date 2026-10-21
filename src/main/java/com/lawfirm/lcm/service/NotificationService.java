package com.lawfirm.lcm.service;

import com.lawfirm.lcm.domain.Notification;
import com.lawfirm.lcm.domain.User;
import com.lawfirm.lcm.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public void notify(User user, String message, String link) {
        Notification n = new Notification();
        n.setUser(user);
        n.setMessage(message);
        n.setLink(link);
        notificationRepository.save(n);
    }

    public List<Notification> forUser(Long userId) {
        return notificationRepository.findByUser_IdOrderByCreatedAtDesc(userId);
    }

    public long unreadCount(Long userId) {
        return notificationRepository.countByUser_IdAndReadFalse(userId);
    }

    public void markRead(Notification n) {
        n.setRead(true);
        notificationRepository.save(n);
    }
}

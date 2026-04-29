package com.example.vag.service.impl;

import com.example.vag.model.Notification;
import com.example.vag.model.User;
import com.example.vag.repository.NotificationRepository;
import com.example.vag.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Collection;

@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationServiceImpl(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public Notification create(User user, String message, String targetLink) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setMessage(message);
        notification.setTargetLink(targetLink);
        return notificationRepository.save(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> findRecent(User user, int limit) {
        if (limit <= 5) {
            return notificationRepository.findTop5ByUserOrderByCreatedAtDesc(user);
        }
        return notificationRepository.findByUserOrderByCreatedAtDesc(user, Pageable.ofSize(limit)).getContent();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Notification> findAll(User user, Pageable pageable) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnread(User user) {
        return notificationRepository.countByUserAndReadFalse(user);
    }

    @Override
    public void markAllAsRead(User user) {
        notificationRepository.markAllAsReadByUser(user);
    }

    @Override
    public boolean deleteForUser(User user, Long notificationId) {
        return notificationRepository.deleteByIdAndUser(notificationId, user) > 0;
    }

    @Override
    public long deleteForUser(User user, Collection<Long> notificationIds) {
        if (notificationIds == null || notificationIds.isEmpty()) {
            return 0;
        }
        return notificationRepository.deleteByUserAndIdIn(user, notificationIds);
    }
}

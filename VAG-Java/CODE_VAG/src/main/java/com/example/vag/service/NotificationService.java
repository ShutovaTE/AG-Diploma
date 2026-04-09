package com.example.vag.service;

import com.example.vag.model.Notification;
import com.example.vag.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface NotificationService {
    Notification create(User user, String message, String targetLink);
    List<Notification> findRecent(User user, int limit);
    Page<Notification> findAll(User user, Pageable pageable);
    long countUnread(User user);
    void markAllAsRead(User user);
}

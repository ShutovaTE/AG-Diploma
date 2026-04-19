package com.example.vag.controller;

import com.example.vag.model.User;
import com.example.vag.service.NotificationService;
import com.example.vag.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributes {

    private final UserService userService;
    private final NotificationService notificationService;

    public GlobalModelAttributes(UserService userService, NotificationService notificationService) {
        this.userService = userService;
        this.notificationService = notificationService;
    }

    @ModelAttribute("recentNotifications")
    public Object recentNotifications() {
        User user = resolveCurrentUser();
        if (user == null) {
            return null;
        }
        return notificationService.findRecent(user, 5);
    }

    @ModelAttribute("unreadNotificationsCount")
    public long unreadNotificationsCount() {
        User user = resolveCurrentUser();
        if (user == null) {
            return 0L;
        }
        return notificationService.countUnread(user);
    }

    private User resolveCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        String username = authentication.getName();
        if (username == null || "anonymousUser".equalsIgnoreCase(username)) {
            return null;
        }

        return userService.findByUsername(username).orElse(null);
    }
}

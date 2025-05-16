package com.africa.hr.controller;

import com.africa.hr.dto.NotificationDto;
import com.africa.hr.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing notifications.
 */
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Gets all notifications for the current authenticated user.
     *
     * @return ResponseEntity with the list of notifications
     */
    @GetMapping
    public ResponseEntity<List<NotificationDto>> getCurrentUserNotifications() {
        List<NotificationDto> notifications = notificationService.getCurrentUserNotifications();
        return ResponseEntity.ok(notifications);
    }

    /**
     * Gets all notifications for a specific user (admin access required).
     *
     * @param userId the user ID
     * @return ResponseEntity with the list of notifications
     */
    @GetMapping("/users/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<NotificationDto>> getUserNotifications(@PathVariable Long userId) {
        List<NotificationDto> notifications = notificationService.getUserNotifications(userId);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Marks a notification as read.
     *
     * @param notificationId the notification ID
     * @return ResponseEntity with the updated notification
     */
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<NotificationDto> markNotificationAsRead(@PathVariable Long notificationId) {
        NotificationDto notification = notificationService.markAsRead(notificationId);
        return ResponseEntity.ok(notification);
    }

    /**
     * Marks all notifications for the current authenticated user as read.
     *
     * @return ResponseEntity with the number of notifications marked as read
     */
    @PatchMapping("/mark-all-as-read")
    public ResponseEntity<Integer> markAllAsRead() {
        int count = notificationService.markAllAsRead();
        return ResponseEntity.ok(count);
    }

    /**
     * Creates a notification for a user (admin access required).
     * This endpoint is mainly for testing purposes.
     *
     * @param userId  the user ID
     * @param title   the notification title
     * @param message the notification message
     * @return ResponseEntity with the created notification
     */
    @PostMapping("/user/{userId}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<NotificationDto> createNotification(
            @PathVariable Long userId,
            @RequestParam String title,
            @RequestParam String message) {
        NotificationDto notification = notificationService.createNotification(userId, title, message);
        return new ResponseEntity<>(notification, HttpStatus.CREATED);
    }
}
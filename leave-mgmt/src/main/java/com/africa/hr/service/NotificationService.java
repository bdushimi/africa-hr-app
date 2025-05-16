package com.africa.hr.service;

import com.africa.hr.dto.NotificationDto;
import com.africa.hr.model.User;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for managing notifications.
 */
public interface NotificationService {

    /**
     * Creates a notification for a specific user.
     *
     * @param userId  the ID of the user to be notified
     * @param title   the notification title
     * @param message the notification message
     * @return the created notification DTO
     */
    NotificationDto createNotification(Long userId, String title, String message);

    /**
     * Creates a notification for a specific user.
     *
     * @param user    the user to be notified
     * @param title   the notification title
     * @param message the notification message
     * @return the created notification DTO
     */
    NotificationDto createNotification(User user, String title, String message);

    /**
     * Gets all notifications for the current authenticated user.
     *
     * @return list of notification DTOs
     */
    List<NotificationDto> getCurrentUserNotifications();

    /**
     * Gets all notifications for a specific user.
     *
     * @param userId the user ID
     * @return list of notification DTOs
     */
    List<NotificationDto> getUserNotifications(Long userId);

    /**
     * Deletes a notification.
     *
     * @param notificationId the notification ID
     */
    void deleteNotification(Long notificationId);

    /**
     * Marks a notification as read.
     *
     * @param notificationId the notification ID
     * @return the updated notification DTO
     */
    NotificationDto markAsRead(Long notificationId);

    /**
     * Marks all notifications for the current authenticated user as read.
     *
     * @return the number of notifications marked as read
     */
    int markAllAsRead();
}
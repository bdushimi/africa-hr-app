package com.africa.hr.websocket;

import com.africa.hr.dto.NotificationDto;
import com.africa.hr.model.Notification;
import com.africa.hr.model.User;
import com.africa.hr.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Service for sending real-time notifications over WebSocket connections.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;
    private final WebSocketSessionRegistry sessionRegistry;

    /**
     * Sends a notification to a specific user. If the user is currently connected,
     * the notification is sent in real-time via WebSocket. Either way, the
     * notification
     * is saved to the database for the user to retrieve later.
     *
     * @param user    the user to notify
     * @param title   the notification title
     * @param message the notification message
     * @return the created notification DTO
     */
    public NotificationDto sendNotification(User user, String title, String message) {
        // Always save the notification to the database
        NotificationDto notification = notificationService.createNotification(user, title, message);

        // Check if the user is currently connected
        if (sessionRegistry.isUserConnected(user.getId())) {
            // Send the notification in real-time
            String destination = "/user/" + user.getId() + "/queue/notifications";
            messagingTemplate.convertAndSend(destination, notification);
            log.info("Real-time notification sent to user ID: {}", user.getId());
        } else {
            log.info("User ID: {} is not connected. Notification saved to database only.", user.getId());
        }

        return notification;
    }

    /**
     * Sends a notification to a specific user by ID. If the user is currently
     * connected,
     * the notification is sent in real-time via WebSocket. Either way, the
     * notification
     * is saved to the database for the user to retrieve later.
     *
     * @param userId  the ID of the user to notify
     * @param title   the notification title
     * @param message the notification message
     * @return the created notification DTO
     */
    public NotificationDto sendNotification(Long userId, String title, String message) {
        // Always save the notification to the database
        NotificationDto notification = notificationService.createNotification(userId, title, message);

        // Check if the user is currently connected
        if (sessionRegistry.isUserConnected(userId)) {
            // Send the notification in real-time
            String destination = "/user/" + userId + "/queue/notifications";
            messagingTemplate.convertAndSend(destination, notification);
            log.info("Real-time notification sent to user ID: {}", userId);
        } else {
            log.info("User ID: {} is not connected. Notification saved to database only.", userId);
        }

        return notification;
    }

    /**
     * Broadcasts a notification to all connected users with a specific role.
     *
     * @param roleName the role name (e.g., "ROLE_MANAGER")
     * @param title    the notification title
     * @param message  the notification message
     */
    public void broadcastToRole(String roleName, String title, String message) {
        messagingTemplate.convertAndSend("/topic/role/" + roleName,
                new BroadcastNotification(title, message));
        log.info("Broadcast notification sent to role: {}", roleName);
    }

    /**
     * Broadcasts a notification to all connected users.
     *
     * @param title   the notification title
     * @param message the notification message
     */
    public void broadcastToAll(String title, String message) {
        messagingTemplate.convertAndSend("/topic/global",
                new BroadcastNotification(title, message));
        log.info("Global broadcast notification sent");
    }

    /**
     * Simple DTO for broadcast notifications that don't need to be persisted.
     */
    private record BroadcastNotification(String title, String message) {
    }
}
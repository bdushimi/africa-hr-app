package com.africa.hr.websocket;

import com.africa.hr.dto.NotificationDto;
import com.africa.hr.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;

import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.List;

/**
 * Controller for handling WebSocket messages.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {

    private final NotificationService notificationService;

    /**
     * Endpoint for clients to request their unread notifications.
     * The client calls this by sending a message to /app/notifications.
     *
     * @param principal the authenticated user
     * @return list of notifications for the user
     */
    @MessageMapping("/notifications")
    @SendToUser("/queue/notifications")
    public List<NotificationDto> getUserNotifications(Principal principal) {
        log.info("WebSocket request for notifications from user: {}", principal.getName());
        return notificationService.getCurrentUserNotifications();
    }

    /**
     * Endpoint for marking a notification as read through WebSocket.
     * The client calls this by sending a message to /app/notifications/mark-read.
     *
     * @param notificationId the notification ID
     * @return the updated notification
     */
    @MessageMapping("/notifications/mark-read")
    @SendToUser("/queue/notification-read")
    public NotificationDto markNotificationAsRead(@Payload Long notificationId, Principal principal) {
        log.info("WebSocket request to mark notification {} as read from user: {}", notificationId,
                principal.getName());

        return notificationService.markAsRead(notificationId);
    }

    /**
     * Endpoint for marking all notifications as read through WebSocket.
     * The client calls this by sending a message to
     * /app/notifications/mark-all-read.
     *
     * @param principal the authenticated user
     * @return the number of notifications marked as read
     */
    @MessageMapping("/notifications/mark-all-read")
    @SendToUser("/queue/notifications-all-read")
    public Integer markAllNotificationsAsRead(Principal principal) {
        log.info("WebSocket request to mark all notifications as read from user: {}",
                principal.getName());

        return notificationService.markAllAsRead();
    }
}
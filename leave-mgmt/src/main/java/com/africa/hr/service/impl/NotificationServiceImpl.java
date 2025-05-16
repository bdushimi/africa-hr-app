package com.africa.hr.service.impl;

import com.africa.hr.dto.NotificationDto;
import com.africa.hr.exception.ResourceNotFoundException;
import com.africa.hr.model.Notification;
import com.africa.hr.model.User;
import com.africa.hr.repository.NotificationRepository;
import com.africa.hr.repository.UserRepository;
import com.africa.hr.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;

import java.util.stream.Collectors;

/**
 * Implementation of the NotificationService interface.
 */
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public NotificationDto createNotification(Long userId, String title, String message) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        return createNotification(user, title, message);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public NotificationDto createNotification(User user, String title, String message) {
        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .isRead(false)
                .build();

        Notification savedNotification = notificationRepository.save(notification);
        return NotificationDto.fromEntity(savedNotification);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<NotificationDto> getCurrentUserNotifications() {
        User currentUser = getCurrentAuthenticatedUser();
        return getUserNotifications(currentUser);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<NotificationDto> getUserNotifications(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        return getUserNotifications(user);
    }

    /**
     * Get notifications for a specific user.
     *
     * @param user the user
     * @return list of notification DTOs
     */
    private List<NotificationDto> getUserNotifications(User user) {
        List<Notification> notifications = notificationRepository.findByUserOrderByIsReadAscCreatedAtDesc(user);
        return notifications.stream()
                .map(NotificationDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void deleteNotification(Long notificationId) {
        User currentUser = getCurrentAuthenticatedUser();

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        // Ensure the current user owns the notification
        if (!notification.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You are not authorized to delete this notification");
        }

        notificationRepository.delete(notification);
    }

    @Override
    @Transactional
    public NotificationDto markAsRead(Long notificationId) {
        User currentUser = getCurrentAuthenticatedUser();

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));

        // Security check - ensure the notification belongs to the current user
        if (!notification.getUser().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("You don't have permission to access this notification");
        }

        notification.setRead(true);
        Notification updatedNotification = notificationRepository.save(notification);

        return NotificationDto.fromEntity(updatedNotification);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public int markAllAsRead() {
        User currentUser = getCurrentAuthenticatedUser();
        return notificationRepository.markAllAsRead(currentUser);
    }

    /**
     * Gets the current authenticated user.
     *
     * @return the current authenticated user
     * @throws ResourceNotFoundException if the user is not found
     */
    private User getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }
}
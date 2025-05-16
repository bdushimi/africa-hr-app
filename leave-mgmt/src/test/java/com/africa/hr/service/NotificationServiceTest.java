package com.africa.hr.service;

import com.africa.hr.dto.NotificationDto;
import com.africa.hr.model.Notification;
import com.africa.hr.model.Role;
import com.africa.hr.model.User;
import com.africa.hr.repository.NotificationRepository;
import com.africa.hr.repository.UserRepository;
import com.africa.hr.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private User testUser;
    private Notification testNotification;
    private Long notificationId;

    @BeforeEach
    void setUp() {
        // Setup security context
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(authentication.getName()).thenReturn("test@example.com");

        // Create role
        Role userRole = new Role();
        userRole.setId(1L);
        userRole.setName("ROLE_USER");

        // Setup test user
        testUser = User.builder()
                .id(1L)
                .firstName("Test")
                .lastName("User")
                .email("test@example.com")
                .password("password")
                .joinedDate(LocalDate.now())
                .status(User.Status.ACTIVE)
                .role(userRole)
                .build();

        // Setup test notification
        notificationId = 1L;
        testNotification = Notification.builder()
                .id(notificationId)
                .user(testUser)
                .title("Test Notification")
                .message("This is a test notification")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
    }

    @Test
    void createNotification_ShouldCreateAndReturnNotificationDto() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        // Act
        NotificationDto result = notificationService.createNotification(1L, "Test Notification",
                "This is a test notification");

        // Assert
        assertNotNull(result);
        assertEquals(notificationId, result.getId());
        assertEquals("Test Notification", result.getTitle());
        assertEquals("This is a test notification", result.getMessage());
        assertEquals(1L, result.getUserId());
        assertFalse(result.isRead());
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void getCurrentUserNotifications_ShouldReturnUserNotifications() {
        // Arrange
        List<Notification> notifications = Arrays.asList(testNotification);
        when(notificationRepository.findByUserOrderByIsReadAscCreatedAtDesc(testUser)).thenReturn(notifications);

        // Act
        List<NotificationDto> result = notificationService.getCurrentUserNotifications();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(notificationId, result.get(0).getId());
        assertEquals("Test Notification", result.get(0).getTitle());
        verify(notificationRepository, times(1)).findByUserOrderByIsReadAscCreatedAtDesc(testUser);
    }

    @Test
    void markAsRead_ShouldMarkNotificationAsRead() {
        // Arrange
        when(notificationRepository.findById(any(Long.class))).thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notification.setRead(true);
            return notification;
        });

        // Act
        NotificationDto result = notificationService.markAsRead(notificationId);

        // Assert
        assertNotNull(result);
        assertTrue(result.isRead());
        verify(notificationRepository, times(1)).findById(notificationId);
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void markAllAsRead_ShouldReturnNumberOfNotificationsMarkedAsRead() {
        // Arrange
        when(notificationRepository.markAllAsRead(testUser)).thenReturn(5);

        // Act
        int result = notificationService.markAllAsRead();

        // Assert
        assertEquals(5, result);
        verify(notificationRepository, times(1)).markAllAsRead(testUser);
    }
}
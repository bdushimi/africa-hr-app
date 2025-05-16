package com.africa.hr.repository;

import com.africa.hr.model.Notification;
import com.africa.hr.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Find all notifications for a specific user, ordered by read status (unread
     * first) and creation time (newest first)
     * 
     * @param user the user
     * @return list of notifications
     */
    List<Notification> findByUserOrderByIsReadAscCreatedAtDesc(User user);

    /**
     * Mark all notifications for a user as read
     * 
     * @param user the user
     * @return the number of notifications updated
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.updatedAt = CURRENT_TIMESTAMP WHERE n.user = :user AND n.isRead = false")
    int markAllAsRead(@Param("user") User user);
}
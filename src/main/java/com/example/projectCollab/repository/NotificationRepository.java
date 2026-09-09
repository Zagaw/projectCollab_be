package com.example.projectCollab.repository;

import com.example.projectCollab.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("SELECT n FROM Notification n LEFT JOIN FETCH n.actor WHERE n.recipient.userId = :userId ORDER BY n.createdAt DESC")
    List<Notification> findByRecipientUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT n FROM Notification n LEFT JOIN FETCH n.actor WHERE n.recipient.userId = :userId AND n.isRead = false ORDER BY n.createdAt DESC")
    List<Notification> findUnreadByRecipientUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.recipient.userId = :userId AND n.isRead = false")
    long countUnreadByRecipientUserId(@Param("userId") Long userId);

    @Query("SELECT n FROM Notification n LEFT JOIN FETCH n.actor WHERE n.notificationId = :notificationId AND n.recipient.userId = :userId")
    Optional<Notification> findByNotificationIdAndRecipientUserId(
            @Param("notificationId") Long notificationId,
            @Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.recipient.userId = :userId AND n.isRead = false")
    int markAllReadForUser(@Param("userId") Long userId);
}

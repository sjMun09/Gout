package com.gout.dao;

import com.gout.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface NotificationRepository extends JpaRepository<Notification, String> {

    Page<Notification> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    long countByUserIdAndReadAtIsNull(String userId);

    @Modifying
    @Query("UPDATE Notification n SET n.readAt = :readAt " +
           "WHERE n.userId = :userId AND n.readAt IS NULL")
    int markAllReadForUser(@Param("userId") String userId,
                           @Param("readAt") LocalDateTime readAt);
}

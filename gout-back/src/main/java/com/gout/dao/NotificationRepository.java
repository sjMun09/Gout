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

    /**
     * 유저 탈퇴 시 해당 사용자가 수신자(user_id)인 알림을 물리 삭제.
     * 탈퇴 후에는 다시 접근할 수 없으므로 보관 가치 없음.
     */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.userId = :userId")
    int deleteByUserId(@Param("userId") String userId);
}

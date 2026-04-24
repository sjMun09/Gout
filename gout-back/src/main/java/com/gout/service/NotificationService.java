package com.gout.service;

import com.gout.dto.response.NotificationResponse;
import com.gout.entity.Notification;
import org.springframework.data.domain.Page;

public interface NotificationService {

    Page<NotificationResponse> list(String userId, int page, int size);

    long unreadCount(String userId);

    void markRead(String id, String userId);

    int markAllRead(String userId);

    /**
     * 다른 서비스가 호출하는 헬퍼. 알림 생성 중 예외가 발생해도
     * 호출자 로직(댓글 작성, 좋아요 등)을 막지 않도록 best-effort 로 동작한다.
     */
    Notification createFor(String userId, String type, String title, String body, String link);
}

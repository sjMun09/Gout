package com.gout.service.impl;

import com.gout.dao.NotificationRepository;
import com.gout.dto.response.NotificationResponse;
import com.gout.entity.Notification;
import com.gout.global.exception.BusinessException;
import com.gout.global.exception.ErrorCode;
import com.gout.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> list(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), size <= 0 ? 20 : size);
        return notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(NotificationResponse::of);
    }

    @Override
    @Transactional(readOnly = true)
    public long unreadCount(String userId) {
        return notificationRepository.countByUserIdAndReadAtIsNull(userId);
    }

    @Override
    @Transactional
    public void markRead(String id, String userId) {
        Notification n = notificationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (!n.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        n.markRead();
    }

    @Override
    @Transactional
    public int markAllRead(String userId) {
        return notificationRepository.markAllReadForUser(userId, LocalDateTime.now());
    }

    @Override
    @Transactional
    public Notification createFor(String userId, String type, String title, String body, String link) {
        if (userId == null || userId.isBlank()) {
            return null;
        }
        try {
            Notification n = Notification.builder()
                    .userId(userId)
                    .type(type)
                    .title(title)
                    .body(body)
                    .link(link)
                    .build();
            return notificationRepository.save(n);
        } catch (Exception e) {
            // 알림 생성 실패가 본 요청(댓글/좋아요) 실패로 이어지지 않도록 swallow
            log.warn("Failed to create notification for user={} type={}: {}",
                    userId, type, e.getMessage());
            return null;
        }
    }
}

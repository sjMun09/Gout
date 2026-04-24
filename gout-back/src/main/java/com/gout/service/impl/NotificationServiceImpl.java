package com.gout.service.impl;

import com.gout.dao.NotificationRepository;
import com.gout.dto.response.NotificationResponse;
import com.gout.entity.Notification;
import com.gout.global.exception.BusinessException;
import com.gout.global.exception.ErrorCode;
import com.gout.service.NotificationService;
import com.gout.util.LogMasks;
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
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public Notification createFor(String userId, String type, String title, String body, String link) {
        if (userId == null || userId.isBlank()) {
            return null;
        }
        // NotificationService 인터페이스 계약: best-effort. 실패해도 호출자(댓글 작성/좋아요) 영향 금지.
        // REQUIRES_NEW 로 별도 tx 에서 돌리고 예외는 흡수한다.
        // - CommentServiceImpl 동기 호출 경로: 알림 저장 실패해도 댓글 커밋됨
        // - PostLikedEventListener(AFTER_COMMIT): 본 tx 는 이미 커밋됨, 동일하게 안전
        try {
            Notification n = Notification.builder()
                    .userId(userId)
                    .type(type)
                    .title(title)
                    .body(body)
                    .link(link)
                    .build();
            return notificationRepository.save(n);
        } catch (RuntimeException ex) {
            log.error("Notification create failed userId={} type={} — dropping (best-effort contract)", LogMasks.maskUserId(userId), type, ex);
            return null;
        }
    }
}

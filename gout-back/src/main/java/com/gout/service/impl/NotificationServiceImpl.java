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
        // 호출자(이벤트 리스너 / CommentService) 가 실패 처리 정책을 가진다.
        // - PostLikedEventListener: AFTER_COMMIT 리스너라 예외가 부모 tx 에 영향 없음 → try/catch ERROR 로그
        // - CommentServiceImpl: 댓글 트랜잭션 안에서 호출되므로 실패 시 함께 롤백
        //   (댓글 보존이 우선이면 CommentServiceImpl 도 이벤트 기반으로 전환 필요 — 후속 티켓)
        Notification n = Notification.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .body(body)
                .link(link)
                .build();
        return notificationRepository.save(n);
    }
}

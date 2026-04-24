package com.gout.service.event;

import com.gout.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * PostLikedEvent 수신자.
 *
 * <p>좋아요 트랜잭션이 AFTER_COMMIT 단계에 도달해야 실행되므로, 알림 생성 실패는
 * 좋아요 상태에 영향을 주지 않는다. 알림 생성 중 발생한 예외는 로그만 남긴다
 * (모니터링 알람에 올리기 쉽도록 ERROR 레벨).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostLikedEventListener {

    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPostLiked(PostLikedEvent event) {
        if (event.postAuthorId() == null || event.postAuthorId().isBlank()) {
            return;
        }
        if (event.postAuthorId().equals(event.likerUserId())) {
            // 자기 좋아요는 알림 대상이 아님.
            return;
        }
        try {
            notificationService.createFor(
                    event.postAuthorId(),
                    "POST_LIKE",
                    "게시글에 좋아요가 눌렸습니다",
                    event.postTitle(),
                    "/community/" + event.postId());
        } catch (Exception e) {
            // 알림 실패는 좋아요 자체에 영향 없음 — 로그만 남긴다.
            log.error("Failed to create POST_LIKE notification postId={} authorId={}: {}",
                    event.postId(), event.postAuthorId(), e.getMessage(), e);
        }
    }
}

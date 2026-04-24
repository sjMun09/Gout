package com.gout.service.event;

/**
 * 게시글 좋아요 추가 이벤트.
 *
 * <p>{@code @TransactionalEventListener(AFTER_COMMIT)} 로 수신되어 알림 생성은
 * 좋아요 트랜잭션 바깥에서 처리된다. 알림 저장 실패가 좋아요 롤백으로 번지지 않도록 분리.
 */
public record PostLikedEvent(String postId, String postAuthorId, String likerUserId, String postTitle) {
}

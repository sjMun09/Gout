package com.gout;

import com.gout.dao.NotificationRepository;
import com.gout.dto.request.CreateCommentRequest;
import com.gout.dto.request.CreatePostRequest;
import com.gout.service.CommentService;
import com.gout.service.NotificationService;
import com.gout.service.PostService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 서비스 계층 기반 알림 통합 테스트.
 *
 * /api/auth/register 와 UserRepository.save() 둘 다 gender_type enum 매핑 버그 때문에
 * 실패한다 (NEXT_STEPS.md §1.1 참조). 알림 로직은 users 테이블 존재만 필요하므로
 * JdbcTemplate 로 최소한의 user row 만 직접 삽입해 해당 버그를 우회한다.
 */
class NotificationIntegrationTest extends IntegrationTestBase {

    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired PostService postService;
    @Autowired CommentService commentService;
    @Autowired NotificationService notificationService;
    @Autowired NotificationRepository notificationRepository;

    private String insertUser(String nickname) {
        String id = UUID.randomUUID().toString();
        String email = "notif-" + id + "@gout.test";
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password, nickname, role) " +
                "VALUES (?, ?, ?, ?, 'USER'::user_role)",
                id, email, "x", nickname);
        return id;
    }

    @Test
    @DisplayName("userA 글에 userB 댓글 → userA 미읽음 1 → 읽음 처리 → 0")
    void comment_triggers_notification_for_post_author() {
        String userA = insertUser("userA");
        String userB = insertUser("userB");

        CreatePostRequest postReq = new CreatePostRequest();
        postReq.setTitle("알림 테스트 글");
        postReq.setContent("본문");
        postReq.setCategory("FREE");
        postReq.setAnonymous(false);
        String postId = postService.createPost(userA, postReq).getId();
        assertNotNull(postId);

        long before = notificationService.unreadCount(userA);

        CreateCommentRequest commentReq = new CreateCommentRequest();
        commentReq.setContent("안녕하세요");
        commentReq.setAnonymous(false);
        commentService.createComment(postId, userB, commentReq);

        assertEquals(before + 1, notificationService.unreadCount(userA));
        // userB 본인에겐 증가 없음
        assertEquals(0, notificationService.unreadCount(userB));

        int updated = notificationService.markAllRead(userA);
        assertEquals(before + 1, updated);
        assertEquals(0, notificationService.unreadCount(userA));
    }

    @Test
    @DisplayName("자기 글에 자기 댓글은 알림 생성하지 않음")
    void self_comment_does_not_notify() {
        String user = insertUser("self");

        CreatePostRequest postReq = new CreatePostRequest();
        postReq.setTitle("셀프");
        postReq.setContent("셀프 본문");
        postReq.setCategory("FREE");
        postReq.setAnonymous(false);
        String postId = postService.createPost(user, postReq).getId();

        CreateCommentRequest commentReq = new CreateCommentRequest();
        commentReq.setContent("내 댓글");
        commentReq.setAnonymous(false);
        commentService.createComment(postId, user, commentReq);

        assertEquals(0, notificationService.unreadCount(user));
    }

    @Test
    @DisplayName("좋아요 추가 시 게시글 작성자에게 POST_LIKE 알림, 자기 좋아요는 제외")
    void like_triggers_notification_for_post_author() {
        String author = insertUser("author");
        String liker = insertUser("liker");

        CreatePostRequest postReq = new CreatePostRequest();
        postReq.setTitle("좋아요 테스트");
        postReq.setContent("본문");
        postReq.setCategory("FREE");
        postReq.setAnonymous(false);
        String postId = postService.createPost(author, postReq).getId();

        long before = notificationService.unreadCount(author);

        postService.toggleLike(postId, author);
        assertEquals(before, notificationService.unreadCount(author));

        postService.toggleLike(postId, liker);
        assertEquals(before + 1, notificationService.unreadCount(author));

        // 좋아요 해제는 알림을 추가하지 않음
        postService.toggleLike(postId, liker);
        assertEquals(before + 1, notificationService.unreadCount(author));
    }
}

package com.gout.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 게시글 북마크(스크랩).
 * - 한 사용자는 같은 게시글을 중복 북마크할 수 없다 (UNIQUE user_id, post_id).
 * - 토글 방식으로 삭제 후 재생성되므로 자체 PK(id)를 둔다.
 */
@Entity
@Table(
        name = "post_bookmarks",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_post_bookmarks_user_post",
                columnNames = {"user_id", "post_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostBookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "post_id", nullable = false, length = 36)
    private String postId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private PostBookmark(String userId, String postId) {
        this.userId = userId;
        this.postId = postId;
    }

    public static PostBookmark of(String userId, String postId) {
        return new PostBookmark(userId, postId);
    }

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}

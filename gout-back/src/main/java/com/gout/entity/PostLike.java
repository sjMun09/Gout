package com.gout.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "post_likes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostLike {

    @EmbeddedId
    private PostLikeId id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private PostLike(PostLikeId id) {
        this.id = id;
    }

    public static PostLike of(String postId, String userId) {
        return new PostLike(new PostLikeId(postId, userId));
    }

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    @Embeddable
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @EqualsAndHashCode
    public static class PostLikeId implements Serializable {

        @Column(name = "post_id", nullable = false)
        private String postId;

        @Column(name = "user_id", nullable = false)
        private String userId;

        public PostLikeId(String postId, String userId) {
            this.postId = Objects.requireNonNull(postId);
            this.userId = Objects.requireNonNull(userId);
        }
    }
}

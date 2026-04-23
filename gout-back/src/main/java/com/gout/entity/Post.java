package com.gout.entity;

import com.gout.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PostCategory category;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "view_count", nullable = false)
    private int viewCount;

    @Column(name = "like_count", nullable = false)
    private int likeCount;

    @Column(name = "is_anonymous", nullable = false)
    private boolean isAnonymous;

    @Column(nullable = false, length = 20)
    private String status;

    @Builder
    private Post(String userId, PostCategory category, String title, String content,
                 boolean isAnonymous) {
        this.userId = userId;
        this.category = category != null ? category : PostCategory.FREE;
        this.title = title;
        this.content = content;
        this.isAnonymous = isAnonymous;
        this.viewCount = 0;
        this.likeCount = 0;
        this.status = "VISIBLE";
    }

    public void incrementViewCount() {
        this.viewCount++;
    }

    public void toggleLike(boolean liked) {
        if (liked) {
            this.likeCount++;
        } else if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

    public void updateContent(String title, String content) {
        if (title != null && !title.isBlank()) {
            this.title = title;
        }
        if (content != null && !content.isBlank()) {
            this.content = content;
        }
    }

    public void delete() {
        this.status = "DELETED";
    }

    public enum PostCategory {
        HOSPITAL_REVIEW,
        FOOD_EXPERIENCE,
        EXERCISE,
        MEDICATION,
        QUESTION,
        SUCCESS_STORY,
        FREE
    }
}

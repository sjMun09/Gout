package com.gout.entity;

import com.gout.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    /**
     * 게시글에 첨부된 이미지 URL 목록.
     * Postgres TEXT[] 로 저장. 값은 백엔드가 내려주는 상대 URL
     * (예: /api/uploads/posts/abc123.jpg).
     */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "image_urls", columnDefinition = "text[]")
    private List<String> imageUrls = new ArrayList<>();

    @Builder
    private Post(String userId, PostCategory category, String title, String content,
                 boolean isAnonymous, List<String> imageUrls) {
        this.userId = userId;
        this.category = category != null ? category : PostCategory.FREE;
        this.title = title;
        this.content = content;
        this.isAnonymous = isAnonymous;
        this.viewCount = 0;
        this.likeCount = 0;
        this.status = Status.VISIBLE;
        this.imageUrls = imageUrls != null ? new ArrayList<>(imageUrls) : new ArrayList<>();
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

    /**
     * 이미지 URL 전체 교체. null 이 넘어오면 기존 값을 유지한다
     * (클라이언트가 이미지 필드를 생략한 업데이트 요청일 수 있음).
     */
    public void replaceImageUrls(List<String> imageUrls) {
        if (imageUrls == null) {
            return;
        }
        this.imageUrls = new ArrayList<>(imageUrls);
    }

    public void delete() {
        this.status = Status.DELETED;
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

    /**
     * 게시글 상태. DB 컬럼은 VARCHAR(20) — EnumType.STRING 으로 enum.name() 보존.
     * VISIBLE: 일반 노출, HIDDEN: 관리자 가림(현재 native UPDATE 만 사용),
     * DELETED: Soft delete.
     */
    public enum Status {
        VISIBLE, HIDDEN, DELETED
    }
}

package com.gout.entity;

import com.gout.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "comments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "post_id", nullable = false)
    private String postId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "parent_id")
    private String parentId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_anonymous", nullable = false)
    private boolean isAnonymous;

    @Column(nullable = false, length = 20)
    private String status;

    @Builder
    private Comment(String postId, String userId, String parentId, String content,
                    boolean isAnonymous) {
        this.postId = postId;
        this.userId = userId;
        this.parentId = parentId;
        this.content = content;
        this.isAnonymous = isAnonymous;
        this.status = "VISIBLE";
    }

    public void delete() {
        this.status = "DELETED";
    }

    public void edit(String content) {
        // BaseEntity.updatedAt 은 JPA auditing 으로 자동 갱신되므로 content 만 변경.
        this.content = content;
    }
}

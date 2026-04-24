package com.gout.dto.response;

import com.gout.entity.Comment;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CommentResponse {

    private final String id;
    private final String postId;
    private final String parentId;
    private final String userId;
    private final String content;
    private final boolean isAnonymous;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final String nickname;

    public static CommentResponse of(Comment comment, String nickname) {
        return CommentResponse.builder()
                .id(comment.getId())
                .postId(comment.getPostId())
                .parentId(comment.getParentId())
                .userId(comment.getUserId())
                .content(comment.getContent())
                .isAnonymous(comment.isAnonymous())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .nickname(comment.isAnonymous() ? "익명" : nickname)
                .build();
    }
}

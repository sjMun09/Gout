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
    private final String content;
    private final boolean isAnonymous;
    private final LocalDateTime createdAt;
    private final String nickname;

    public static CommentResponse of(Comment comment, String nickname) {
        return CommentResponse.builder()
                .id(comment.getId())
                .postId(comment.getPostId())
                .parentId(comment.getParentId())
                .content(comment.getContent())
                .isAnonymous(comment.isAnonymous())
                .createdAt(comment.getCreatedAt())
                .nickname(comment.isAnonymous() ? "익명" : nickname)
                .build();
    }
}

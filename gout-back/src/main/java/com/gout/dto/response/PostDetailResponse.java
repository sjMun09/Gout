package com.gout.dto.response;

import com.gout.entity.Post;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class PostDetailResponse {

    private final String id;
    private final String title;
    private final String category;
    private final String content;
    private final int viewCount;
    private final int likeCount;
    private final int commentCount;
    private final boolean isAnonymous;
    private final LocalDateTime createdAt;
    private final String nickname;
    private final boolean liked;
    private final List<CommentResponse> comments;

    public static PostDetailResponse of(Post post, String nickname, boolean liked,
                                        List<CommentResponse> comments) {
        return PostDetailResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .category(post.getCategory().name())
                .content(post.getContent())
                .viewCount(post.getViewCount())
                .likeCount(post.getLikeCount())
                .commentCount(comments != null ? comments.size() : 0)
                .isAnonymous(post.isAnonymous())
                .createdAt(post.getCreatedAt())
                .nickname(post.isAnonymous() ? "익명" : nickname)
                .liked(liked)
                .comments(comments)
                .build();
    }
}

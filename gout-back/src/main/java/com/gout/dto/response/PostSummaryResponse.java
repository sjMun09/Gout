package com.gout.dto.response;

import com.gout.entity.Post;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class PostSummaryResponse {

    private final String id;
    private final String title;
    private final String category;
    private final int viewCount;
    private final int likeCount;
    private final int commentCount;
    private final boolean isAnonymous;
    private final LocalDateTime createdAt;
    private final String nickname;
    private final List<String> imageUrls;
    private final List<String> tags;

    public static PostSummaryResponse of(Post post, int commentCount, String nickname) {
        return of(post, commentCount, nickname, List.of());
    }

    public static PostSummaryResponse of(Post post, int commentCount, String nickname,
                                         List<String> tags) {
        return PostSummaryResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .category(post.getCategory().name())
                .viewCount(post.getViewCount())
                .likeCount(post.getLikeCount())
                .commentCount(commentCount)
                .isAnonymous(post.isAnonymous())
                .createdAt(post.getCreatedAt())
                .nickname(post.isAnonymous() ? "익명" : nickname)
                .imageUrls(post.getImageUrls() != null ? List.copyOf(post.getImageUrls()) : List.of())
                .tags(tags != null ? List.copyOf(tags) : List.of())
                .build();
    }
}

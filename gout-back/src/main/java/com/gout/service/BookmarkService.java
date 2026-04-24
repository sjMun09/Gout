package com.gout.service;

import com.gout.dto.response.PostSummaryResponse;
import org.springframework.data.domain.Page;

public interface BookmarkService {

    /**
     * 북마크 토글. 없으면 생성(true 반환), 있으면 삭제(false 반환).
     */
    boolean toggle(String postId, String userId);

    /**
     * 내 북마크 목록. 북마크 생성 시각 내림차순.
     */
    Page<PostSummaryResponse> getMyBookmarks(String userId, int page, int size);

    /**
     * 현재 사용자의 해당 게시글 북마크 여부.
     */
    boolean isBookmarked(String postId, String userId);
}

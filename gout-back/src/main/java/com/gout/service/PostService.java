package com.gout.service;

import com.gout.dto.request.CreatePostRequest;
import com.gout.dto.response.PostDetailResponse;
import com.gout.dto.response.PostSummaryResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface PostService {

    Page<PostSummaryResponse> getPosts(String category, int page, int size);

    Page<PostSummaryResponse> getPosts(String category, String keyword, int page, int size);

    /**
     * sort: "latest" (default) / "popular" / "views". 알 수 없는 값은 latest 로 fallback.
     */
    Page<PostSummaryResponse> getPosts(String category, String keyword, String sort, int page, int size);

    PostDetailResponse getPost(String id, String currentUserId);

    PostSummaryResponse createPost(String userId, CreatePostRequest request);

    PostDetailResponse updatePost(String id, String userId, CreatePostRequest request);

    void deletePost(String id, String userId);

    void toggleLike(String postId, String userId);

    List<PostSummaryResponse> getTrending(int days, int limit);
}

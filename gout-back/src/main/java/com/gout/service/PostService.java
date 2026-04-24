package com.gout.service;

import com.gout.dto.request.CreatePostRequest;
import com.gout.dto.response.PostDetailResponse;
import com.gout.dto.response.PostSummaryResponse;
import org.springframework.data.domain.Page;

public interface PostService {

    Page<PostSummaryResponse> getPosts(String category, int page, int size);

    Page<PostSummaryResponse> getPosts(String category, String keyword, int page, int size);

    PostDetailResponse getPost(String id, String currentUserId);

    PostSummaryResponse createPost(String userId, CreatePostRequest request);

    PostDetailResponse updatePost(String id, String userId, CreatePostRequest request);

    void deletePost(String id, String userId);

    void toggleLike(String postId, String userId);
}

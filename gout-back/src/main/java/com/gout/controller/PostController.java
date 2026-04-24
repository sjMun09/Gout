package com.gout.controller;

import com.gout.dto.request.CreatePostRequest;
import com.gout.dto.response.PostDetailResponse;
import com.gout.dto.response.PostSummaryResponse;
import com.gout.global.response.ApiResponse;
import com.gout.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<PostSummaryResponse>>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                ApiResponse.success(postService.getPosts(category, keyword, page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PostDetailResponse>> detail(@PathVariable String id) {
        String userId = getCurrentUserIdOrNull();
        return ResponseEntity.ok(ApiResponse.success(postService.getPost(id, userId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PostSummaryResponse>> create(
            @Valid @RequestBody CreatePostRequest request) {
        String userId = getCurrentUserId();
        return ResponseEntity.ok(
                ApiResponse.success("게시글이 작성되었습니다.", postService.createPost(userId, request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PostDetailResponse>> update(
            @PathVariable String id,
            @Valid @RequestBody CreatePostRequest request) {
        String userId = getCurrentUserId();
        return ResponseEntity.ok(
                ApiResponse.success("게시글이 수정되었습니다.", postService.updatePost(id, userId, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        String userId = getCurrentUserId();
        postService.deletePost(id, userId);
        return ResponseEntity.ok(ApiResponse.success("게시글이 삭제되었습니다.", null));
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<ApiResponse<Void>> toggleLike(@PathVariable String id) {
        String userId = getCurrentUserId();
        postService.toggleLike(id, userId);
        return ResponseEntity.ok(ApiResponse.success("좋아요 상태가 변경되었습니다.", null));
    }

    private String getCurrentUserId() {
        String userId = getCurrentUserIdOrNull();
        if (userId == null) {
            throw new AccessDeniedException("로그인이 필요합니다.");
        }
        return userId;
    }

    private String getCurrentUserIdOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        Object principal = auth.getPrincipal();
        if (principal instanceof UserDetails ud) {
            return ud.getUsername();
        }
        if (principal instanceof String s && !"anonymousUser".equals(s)) {
            return s;
        }
        return null;
    }
}

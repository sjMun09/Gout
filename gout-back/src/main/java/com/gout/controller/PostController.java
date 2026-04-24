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

import java.util.List;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    /**
     * days/limit 이 허용 범위 밖일 때 예외 대신 기본값으로 clamp — API 계층 방어 처리.
     * 비즈니스 로직은 서비스에 위임하지 않고 여기서만 처리한다.
     */
    @GetMapping("/trending")
    public ResponseEntity<ApiResponse<List<PostSummaryResponse>>> trending(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "5") int limit) {
        int safeDays = (days < 1 || days > 30) ? 7 : days;
        int safeLimit = (limit < 1 || limit > 20) ? 5 : limit;
        return ResponseEntity.ok(
                ApiResponse.success(postService.getTrending(safeDays, safeLimit)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<PostSummaryResponse>>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                ApiResponse.success(postService.getPosts(category, keyword, sort, page, size)));
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

package com.gout.controller;

import com.gout.constant.AppConstants;
import com.gout.dto.response.BookmarkStatusResponse;
import com.gout.dto.response.PostSummaryResponse;
import com.gout.global.exception.BusinessException;
import com.gout.global.exception.ErrorCode;
import com.gout.global.response.ApiResponse;
import com.gout.service.BookmarkService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class BookmarkController {

    private final BookmarkService bookmarkService;

    @PostMapping("/posts/{postId}/bookmark")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> toggle(
            @PathVariable String postId) {
        String userId = requireUserId();
        boolean bookmarked = bookmarkService.toggle(postId, userId);
        String message = bookmarked ? "북마크에 추가했습니다." : "북마크를 해제했습니다.";
        return ResponseEntity.ok(
                ApiResponse.success(message, Map.of("bookmarked", bookmarked)));
    }

    @GetMapping("/posts/{postId}/bookmark-status")
    public ResponseEntity<ApiResponse<BookmarkStatusResponse>> status(
            @PathVariable String postId) {
        // GET /api/posts/** 는 SecurityConfig 에서 permitAll 이므로 컨트롤러에서 인증 체크.
        String userId = requireUserId();
        boolean bookmarked = bookmarkService.isBookmarked(postId, userId);
        return ResponseEntity.ok(
                ApiResponse.success(BookmarkStatusResponse.of(bookmarked)));
    }

    @GetMapping("/me/bookmarks")
    public ResponseEntity<ApiResponse<Page<PostSummaryResponse>>> myBookmarks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        // 메모리·네트워크 DoS 방어: size 상한 MAX_PAGE_SIZE, 음수·0은 DEFAULT_PAGE_SIZE
        int safePage = Math.max(page, 0);
        int safeSize = AppConstants.clampSize(size);
        String userId = requireUserId();
        return ResponseEntity.ok(
                ApiResponse.success(bookmarkService.getMyBookmarks(userId, safePage, safeSize)));
    }

    private String requireUserId() {
        String userId = getCurrentUserIdOrNull();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
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

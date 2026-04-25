package com.gout.controller;

import com.gout.constant.AppConstants;
import com.gout.dto.response.NotificationResponse;
import com.gout.global.response.ApiResponse;
import com.gout.security.CurrentUserProvider;
import com.gout.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        // 메모리·네트워크 DoS 방어: size 상한 MAX_PAGE_SIZE, 음수·0은 DEFAULT_PAGE_SIZE
        int safePage = AppConstants.clampPage(page);
        int safeSize = AppConstants.clampSize(size);
        String userId = currentUserProvider.requireUserId();
        return ResponseEntity.ok(ApiResponse.success(
                notificationService.list(userId, safePage, safeSize)));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> unreadCount() {
        String userId = currentUserProvider.requireUserId();
        long count = notificationService.unreadCount(userId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("count", count)));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markRead(@PathVariable String id) {
        String userId = currentUserProvider.requireUserId();
        notificationService.markRead(id, userId);
        return ResponseEntity.ok(ApiResponse.success("읽음 처리되었습니다.", null));
    }

    @PostMapping("/read-all")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> markAllRead() {
        String userId = currentUserProvider.requireUserId();
        int updated = notificationService.markAllRead(userId);
        return ResponseEntity.ok(ApiResponse.success(
                "모든 알림을 읽음 처리했습니다.",
                Map.of("updated", updated)));
    }

}

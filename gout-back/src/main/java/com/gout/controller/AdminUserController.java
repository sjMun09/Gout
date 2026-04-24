package com.gout.controller;

import com.gout.dto.response.AdminUserResponse;
import com.gout.global.response.ApiResponse;
import com.gout.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 유저 관리 API.
 * 전역 설정(SecurityConfig) 에서 /api/admin/** 는 authenticated() 로 로그인만 통과시키고,
 * ADMIN 역할 검사는 메서드 수준 @PreAuthorize 로 수행한다.
 * (EnableMethodSecurity — SecurityConfig 참조)
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminService adminService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AdminUserResponse>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(ApiResponse.success(
                adminService.searchUsers(keyword, page, size)));
    }

    @PostMapping("/{id}/suspend")
    public ResponseEntity<ApiResponse<Void>> suspend(@PathVariable String id) {
        adminService.suspendUser(id);
        return ResponseEntity.ok(ApiResponse.success("유저를 정지 처리했습니다.", null));
    }

    @PostMapping("/{id}/unsuspend")
    public ResponseEntity<ApiResponse<Void>> unsuspend(@PathVariable String id) {
        adminService.unsuspendUser(id);
        return ResponseEntity.ok(ApiResponse.success("유저 정지를 해제했습니다.", null));
    }

    @PostMapping("/{id}/promote")
    public ResponseEntity<ApiResponse<Void>> promote(@PathVariable String id) {
        adminService.promoteUser(id);
        return ResponseEntity.ok(ApiResponse.success("ADMIN 권한을 부여했습니다.", null));
    }
}

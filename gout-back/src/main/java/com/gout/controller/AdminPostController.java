package com.gout.controller;

import com.gout.global.response.ApiResponse;
import com.gout.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 게시글 조치 API.
 * 삭제/숨김 모두 Post.status 를 갱신 (hard delete 안 함 — 감사 목적).
 */
@RestController
@RequestMapping("/api/admin/posts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminPostController {

    private final AdminService adminService;

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        adminService.deletePost(id);
        return ResponseEntity.ok(ApiResponse.success("게시글을 삭제 처리했습니다.", null));
    }

    @PostMapping("/{id}/hide")
    public ResponseEntity<ApiResponse<Void>> hide(@PathVariable String id) {
        adminService.hidePost(id);
        return ResponseEntity.ok(ApiResponse.success("게시글을 숨김 처리했습니다.", null));
    }
}

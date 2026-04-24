package com.gout.controller;

import com.gout.dto.request.CreateReportRequest;
import com.gout.dto.response.ReportResponse;
import com.gout.global.response.ApiResponse;
import com.gout.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /** 게시글/댓글 신고. 성공 시 201 Created, 중복 시 409 (서비스에서 throw). */
    @PostMapping
    public ResponseEntity<ApiResponse<ReportResponse>> create(
            @Valid @RequestBody CreateReportRequest request) {
        String reporterId = getCurrentUserId();
        ReportResponse created = reportService.create(reporterId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("신고가 접수되었습니다.", created));
    }

    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("로그인이 필요합니다.");
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof UserDetails ud) {
            return ud.getUsername();
        }
        if (principal instanceof String s && !"anonymousUser".equals(s)) {
            return s;
        }
        throw new AccessDeniedException("로그인이 필요합니다.");
    }
}

package com.gout.controller;

import com.gout.dto.response.ReportResponse;
import com.gout.global.response.ApiResponse;
import com.gout.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 신고 처리 API. 목록 / 처리완료 / 기각 3 엔드포인트.
 *
 * <p>권한: ADMIN. 인증/검증 패턴은 다른 admin 컨트롤러와 동일.
 */
@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminReportController {

    private final ReportService reportService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ReportResponse>>> list(
            @RequestParam(defaultValue = "PENDING") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<ReportResponse> result = reportService.listForAdmin(status, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<ApiResponse<Void>> resolve(@PathVariable String id) {
        reportService.resolve(id);
        return ResponseEntity.ok(ApiResponse.success("신고를 처리 완료로 표시했습니다.", null));
    }

    @PostMapping("/{id}/dismiss")
    public ResponseEntity<ApiResponse<Void>> dismiss(@PathVariable String id) {
        reportService.dismiss(id);
        return ResponseEntity.ok(ApiResponse.success("신고를 기각 처리했습니다.", null));
    }
}

package com.gout.controller;

// TODO: Agent-F 의 ReportRepository / Report 엔티티 머지 후 활성화
// ----------------------------------------------------------------------
// 현재 상태: Agent-F 가 reports 테이블 + Report 엔티티 + ReportRepository 를
//  아직 머지하지 않았기 때문에 이 컨트롤러를 활성화하면 컴파일 에러 (Missing bean) 또는
//  런타임 DB 에러가 발생한다.
//  PR 머지 순서에 따라 본 파일을 실제 구현으로 교체한다.
//
// 아래는 Agent-F 머지 후 복구할 뼈대:
//
// import com.gout.global.response.ApiResponse;
// import lombok.RequiredArgsConstructor;
// import org.springframework.data.domain.Page;
// import org.springframework.http.ResponseEntity;
// import org.springframework.security.access.prepost.PreAuthorize;
// import org.springframework.web.bind.annotation.*;
//
// @RestController
// @RequestMapping("/api/admin/reports")
// @RequiredArgsConstructor
// @PreAuthorize("hasRole('ADMIN')")
// public class AdminReportController {
//
//     private final com.gout.service.ReportAdminService reportAdminService; // Agent-F 제공 예정
//
//     @GetMapping
//     public ResponseEntity<ApiResponse<Page<Object>>> list(
//             @RequestParam(defaultValue = "PENDING") String status,
//             @RequestParam(defaultValue = "0") int page,
//             @RequestParam(defaultValue = "20") int size) {
//         return ResponseEntity.ok(ApiResponse.success(
//                 reportAdminService.list(status, page, size)));
//     }
//
//     @PostMapping("/{id}/resolve")
//     public ResponseEntity<ApiResponse<Void>> resolve(@PathVariable String id) {
//         reportAdminService.resolve(id);
//         return ResponseEntity.ok(ApiResponse.success("신고를 처리 완료로 표시했습니다.", null));
//     }
//
//     @PostMapping("/{id}/dismiss")
//     public ResponseEntity<ApiResponse<Void>> dismiss(@PathVariable String id) {
//         reportAdminService.dismiss(id);
//         return ResponseEntity.ok(ApiResponse.success("신고를 반려했습니다.", null));
//     }
// }

package com.gout.controller;

import com.gout.config.openapi.AuthenticatedApiResponses;
import com.gout.dto.response.AdminUserResponse;
import com.gout.global.response.ApiResponse;
import com.gout.global.response.ErrorResponse;
import com.gout.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Admin", description = "관리자 전용 API. 모든 엔드포인트가 ADMIN 역할 필요.")
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminService adminService;

    @Operation(
            summary = "[ADMIN] 사용자 목록 검색",
            description = "닉네임/이메일 부분 일치로 사용자 목록을 검색한다. 페이징 적용.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "검색 결과 페이지.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "ADMIN 역할 없음.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "FORBIDDEN",
                                    value = "{\"success\":false,\"code\":\"COMMON_FORBIDDEN\",\"message\":\"접근 권한이 없습니다.\",\"status\":403,\"path\":\"/api/admin/users\",\"timestamp\":\"2026-01-01T00:00:00Z\"}")))
    })
    @AuthenticatedApiResponses
    @GetMapping
    public ResponseEntity<ApiResponse<Page<AdminUserResponse>>> list(
            @Parameter(description = "0-based 페이지 번호.", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기.", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "검색 키워드(닉네임/이메일 부분 일치).")
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(ApiResponse.success(
                adminService.searchUsers(keyword, page, size)));
    }

    @Operation(summary = "[ADMIN] 사용자 정지", description = "지정한 사용자를 정지 상태로 전환한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "정지 처리됨.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "ADMIN 역할 없음.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "FORBIDDEN",
                                    value = "{\"success\":false,\"code\":\"COMMON_FORBIDDEN\",\"message\":\"접근 권한이 없습니다.\",\"status\":403,\"path\":\"/api/admin/users/abc/suspend\",\"timestamp\":\"2026-01-01T00:00:00Z\"}")))
    })
    @AuthenticatedApiResponses
    @PostMapping("/{id}/suspend")
    public ResponseEntity<ApiResponse<Void>> suspend(
            @Parameter(description = "사용자 ID.") @PathVariable String id) {
        adminService.suspendUser(id);
        return ResponseEntity.ok(ApiResponse.success("유저를 정지 처리했습니다.", null));
    }

    @Operation(summary = "[ADMIN] 사용자 정지 해제", description = "정지된 사용자의 정지 상태를 해제한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "해제 처리됨.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "ADMIN 역할 없음.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "FORBIDDEN",
                                    value = "{\"success\":false,\"code\":\"COMMON_FORBIDDEN\",\"message\":\"접근 권한이 없습니다.\",\"status\":403,\"path\":\"/api/admin/users/abc/unsuspend\",\"timestamp\":\"2026-01-01T00:00:00Z\"}")))
    })
    @AuthenticatedApiResponses
    @PostMapping("/{id}/unsuspend")
    public ResponseEntity<ApiResponse<Void>> unsuspend(
            @Parameter(description = "사용자 ID.") @PathVariable String id) {
        adminService.unsuspendUser(id);
        return ResponseEntity.ok(ApiResponse.success("유저 정지를 해제했습니다.", null));
    }

    @Operation(summary = "[ADMIN] ADMIN 권한 부여", description = "지정한 사용자를 ADMIN 역할로 승격한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "승격 처리됨.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "ADMIN 역할 없음.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "FORBIDDEN",
                                    value = "{\"success\":false,\"code\":\"COMMON_FORBIDDEN\",\"message\":\"접근 권한이 없습니다.\",\"status\":403,\"path\":\"/api/admin/users/abc/promote\",\"timestamp\":\"2026-01-01T00:00:00Z\"}")))
    })
    @AuthenticatedApiResponses
    @PostMapping("/{id}/promote")
    public ResponseEntity<ApiResponse<Void>> promote(
            @Parameter(description = "사용자 ID.") @PathVariable String id) {
        adminService.promoteUser(id);
        return ResponseEntity.ok(ApiResponse.success("ADMIN 권한을 부여했습니다.", null));
    }
}

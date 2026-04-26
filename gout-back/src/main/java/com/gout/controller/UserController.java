package com.gout.controller;

import com.gout.config.openapi.AuthenticatedApiResponses;
import com.gout.dto.request.ChangePasswordRequest;
import com.gout.dto.request.EditProfileRequest;
import com.gout.dto.response.UserProfileResponse;
import com.gout.global.response.ApiResponse;
import com.gout.global.response.ErrorResponse;
import com.gout.security.CurrentUserProvider;
import com.gout.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * /api/me — 현재 로그인 사용자의 프로필/비밀번호/탈퇴.
 *
 * AuthController 와 분리 — AuthController 는 회원가입/로그인/토큰 재발급 전용이므로
 * 별도 컨트롤러로 분리하는 것이 책임 분리상 자연스럽다.
 */
@Tag(name = "User", description = "현재 로그인 사용자의 프로필 조회/수정, 비밀번호 변경, 탈퇴.")
@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final CurrentUserProvider currentUserProvider;

    @Operation(summary = "내 프로필 조회", description = "JWT 로 식별된 현재 사용자 프로필을 반환한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "프로필 조회 성공.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)))
    })
    @AuthenticatedApiResponses
    @GetMapping
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMe() {
        return ResponseEntity.ok(ApiResponse.success(userService.getMe(currentUserProvider.requireUserId())));
    }

    @Operation(summary = "내 프로필 수정", description = "닉네임/생년/성별 등 변경 가능 필드만 부분 수정.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "수정 성공.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)))
    })
    @AuthenticatedApiResponses
    @PatchMapping
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateMe(
            @Valid @RequestBody EditProfileRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("프로필이 수정되었습니다.", userService.editProfile(currentUserProvider.requireUserId(), request)));
    }

    @Operation(
            summary = "비밀번호 변경",
            description = "현재 비밀번호 확인 후 새 비밀번호로 교체. 새 비밀번호는 복잡성 정책 충족 필수.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "변경 성공.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "현재 비밀번호 불일치.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "INVALID_CREDENTIALS",
                                    value = "{\"success\":false,\"code\":\"AUTH_INVALID_CREDENTIALS\",\"message\":\"이메일 또는 비밀번호가 올바르지 않습니다.\",\"status\":401,\"path\":\"/api/me/password\",\"timestamp\":\"2026-01-01T00:00:00Z\"}")))
    })
    @AuthenticatedApiResponses
    @PostMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(currentUserProvider.requireUserId(), request);
        return ResponseEntity.ok(ApiResponse.success("비밀번호가 변경되었습니다.", null));
    }

    @Operation(summary = "회원 탈퇴", description = "현재 사용자를 영구 비활성화한다. 복구 불가.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "탈퇴 처리됨.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)))
    })
    @AuthenticatedApiResponses
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> withdraw() {
        userService.withdraw(currentUserProvider.requireUserId());
        return ResponseEntity.ok(ApiResponse.success("회원 탈퇴가 완료되었습니다.", null));
    }
}

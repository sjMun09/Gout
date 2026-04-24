package com.gout.controller;

import com.gout.dto.request.ChangePasswordRequest;
import com.gout.dto.request.EditProfileRequest;
import com.gout.dto.response.UserProfileResponse;
import com.gout.global.exception.BusinessException;
import com.gout.global.exception.ErrorCode;
import com.gout.global.response.ApiResponse;
import com.gout.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * /api/me — 현재 로그인 사용자의 프로필/비밀번호/탈퇴.
 *
 * AuthController 와 분리 — AuthController 는 회원가입/로그인/토큰 재발급 전용이므로
 * 별도 컨트롤러로 분리하는 것이 책임 분리상 자연스럽다.
 */
@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMe() {
        return ResponseEntity.ok(ApiResponse.success(userService.getMe(currentUserId())));
    }

    @PatchMapping
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateMe(
            @Valid @RequestBody EditProfileRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("프로필이 수정되었습니다.", userService.editProfile(currentUserId(), request)));
    }

    @PostMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(currentUserId(), request);
        return ResponseEntity.ok(ApiResponse.success("비밀번호가 변경되었습니다.", null));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> withdraw() {
        userService.withdraw(currentUserId());
        return ResponseEntity.ok(ApiResponse.success("회원 탈퇴가 완료되었습니다.", null));
    }

    private String currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof UserDetails ud) {
            return ud.getUsername();
        }
        if (principal instanceof String s && !"anonymousUser".equals(s)) {
            return s;
        }
        throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }
}

package com.gout.controller;

import com.gout.dto.request.LoginRequest;
import com.gout.dto.request.RefreshRequest;
import com.gout.dto.request.RegisterRequest;
import com.gout.dto.response.TokenResponse;
import com.gout.global.response.ApiResponse;
import com.gout.security.AdminTokenBlacklist;
import com.gout.security.JwtTokenProvider;
import com.gout.service.AuthService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;
    private final AdminTokenBlacklist adminTokenBlacklist;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<TokenResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(ApiResponse.success("회원가입 성공", authService.register(request)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success("로그인 성공", authService.login(request)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(ApiResponse.success("토큰 재발급 성공", authService.refresh(request.getRefreshToken())));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request,
                                                    Authentication authentication) {
        // 인증된 유저의 리프레시 토큰을 Redis 에서 폐기 (P1-8).
        // 미인증 상태(토큰 없음/만료)면 조용히 200 — 프론트가 호출해도 부작용 없음.
        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof UserDetails ud) {
            authService.logout(ud.getUsername());
            // MED-004: ADMIN 권한 유저의 현재 access 토큰을 jti 블랙리스트에 등록.
            // 남은 access 수명 동안 즉시 차단 — logout 직후에도 탈취된 access 를 못 쓴다.
            if (hasAdminAuthority(authentication)) {
                revokeCurrentAdminAccess(request);
            }
        }
        return ResponseEntity.ok(ApiResponse.success("로그아웃 되었습니다.", null));
    }

    private boolean hasAdminAuthority(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(Object::toString)
                .anyMatch(a -> "ROLE_ADMIN".equals(a) || "ADMIN".equals(a));
    }

    private void revokeCurrentAdminAccess(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer == null || !bearer.startsWith("Bearer ")) {
            return;
        }
        String token = bearer.substring(7);
        try {
            JwtTokenProvider.ParsedToken parsed = jwtTokenProvider.parseAccess(token);
            // TTL 은 access 토큰의 설정 상 최대 수명. 실제 남은 수명보다 길 수 있으나
            // 블랙리스트 키가 조금 더 오래 사는 것뿐 — 기능적 영향 없음.
            adminTokenBlacklist.revoke(parsed.getJti(), jwtTokenProvider.getAccessTokenExpirySeconds());
        } catch (JwtException ignored) {
            // logout 시 이미 access 가 만료/변조 상태여도 조용히 무시.
        }
    }
}

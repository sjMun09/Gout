package com.gout.controller;

import com.gout.config.openapi.AuthenticatedApiResponses;
import com.gout.config.openapi.PublicApiResponses;
import com.gout.dto.request.LoginRequest;
import com.gout.dto.request.RefreshRequest;
import com.gout.dto.request.RegisterRequest;
import com.gout.dto.response.TokenResponse;
import com.gout.global.response.ApiResponse;
import com.gout.global.response.ErrorResponse;
import com.gout.security.AdminTokenBlacklist;
import com.gout.security.JwtTokenProvider;
import com.gout.service.AuthService;
import io.jsonwebtoken.JwtException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "회원가입/로그인/토큰 재발급/로그아웃 — JWT 발급 및 폐기 흐름.")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;
    private final AdminTokenBlacklist adminTokenBlacklist;

    @Operation(
            summary = "회원가입",
            description = "이메일/비밀번호/닉네임으로 신규 계정을 생성하고 access/refresh 토큰을 즉시 발급한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "회원가입 성공 — accessToken/refreshToken 반환.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "이미 가입된 이메일.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "DUPLICATE_EMAIL",
                                    value = "{\"success\":false,\"code\":\"AUTH_DUPLICATE_EMAIL\",\"message\":\"이미 가입된 이메일입니다.\",\"status\":400,\"path\":\"/api/auth/register\",\"timestamp\":\"2026-01-01T00:00:00Z\"}")))
    })
    @PublicApiResponses
    @SecurityRequirements({})
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<TokenResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(ApiResponse.success("회원가입 성공", authService.register(request)));
    }

    @Operation(
            summary = "로그인",
            description = "이메일/비밀번호 검증 후 access/refresh 토큰을 발급한다. 레이트 리미트(분당 5회) 적용.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "로그인 성공.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "이메일 또는 비밀번호 불일치.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "INVALID_CREDENTIALS",
                                    value = "{\"success\":false,\"code\":\"AUTH_INVALID_CREDENTIALS\",\"message\":\"이메일 또는 비밀번호가 올바르지 않습니다.\",\"status\":401,\"path\":\"/api/auth/login\",\"timestamp\":\"2026-01-01T00:00:00Z\"}")))
    })
    @PublicApiResponses
    @SecurityRequirements({})
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success("로그인 성공", authService.login(request)));
    }

    @Operation(
            summary = "토큰 재발급",
            description = "유효한 refresh 토큰을 제출해 새 access/refresh 토큰 쌍을 발급받는다(rotating).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "재발급 성공.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "refresh 토큰이 만료/위조/회수됨.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "INVALID_TOKEN",
                                    value = "{\"success\":false,\"code\":\"AUTH_INVALID_TOKEN\",\"message\":\"유효하지 않은 토큰입니다.\",\"status\":401,\"path\":\"/api/auth/refresh\",\"timestamp\":\"2026-01-01T00:00:00Z\"}")))
    })
    @PublicApiResponses
    @SecurityRequirements({})
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(ApiResponse.success("토큰 재발급 성공", authService.refresh(request.getRefreshToken())));
    }

    @Operation(
            summary = "로그아웃",
            description = "현재 로그인 유저의 refresh 토큰을 폐기한다. ADMIN 권한이면 access 토큰도 jti 블랙리스트 등록.\n"
                    + "미인증 상태로 호출돼도 200 — 프론트의 멱등 호출을 허용한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "로그아웃 처리됨.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)))
    })
    @AuthenticatedApiResponses
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

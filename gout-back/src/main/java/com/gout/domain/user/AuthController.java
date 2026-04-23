package com.gout.domain.user;

import com.gout.common.ApiResponse;
import com.gout.domain.user.dto.LoginRequest;
import com.gout.domain.user.dto.RefreshRequest;
import com.gout.domain.user.dto.RegisterRequest;
import com.gout.domain.user.dto.TokenResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<TokenResponse>> register(@Valid @RequestBody RegisterRequest request) {
        TokenResponse tokens = authService.register(request);
        return ResponseEntity.ok(ApiResponse.success("회원가입 성공", tokens));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse tokens = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("로그인 성공", tokens));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(@Valid @RequestBody RefreshRequest request) {
        TokenResponse tokens = authService.refresh(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success("토큰 재발급 성공", tokens));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        // Stateless JWT: 서버 측 세션 없음. 실제 블랙리스트/리프레시 폐기는 후속 구현.
        return ResponseEntity.ok(ApiResponse.success("로그아웃 되었습니다.", null));
    }
}

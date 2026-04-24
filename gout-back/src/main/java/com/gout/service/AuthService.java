package com.gout.service;

import com.gout.dto.request.LoginRequest;
import com.gout.dto.request.RegisterRequest;
import com.gout.dto.response.TokenResponse;

public interface AuthService {

    TokenResponse register(RegisterRequest request);

    TokenResponse login(LoginRequest request);

    TokenResponse refresh(String refreshToken);

    /**
     * 해당 유저의 리프레시 토큰을 폐기(로그아웃). Redis 키 삭제 → 이후 재발급 요청 전부 401.
     */
    void logout(String userId);

    /**
     * 서비스 내부 호출용 — 비밀번호 변경/탈퇴 등 보안 이벤트 시 해당 userId 의
     * 모든 리프레시 세션을 강제 종료한다. 호출자는 해당 사용자의 존재/상태를 이미 검증했다고 가정.
     */
    void forceLogout(String userId);
}

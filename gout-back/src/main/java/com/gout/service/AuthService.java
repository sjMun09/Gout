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
}

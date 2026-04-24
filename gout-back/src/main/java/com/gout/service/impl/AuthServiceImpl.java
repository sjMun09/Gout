package com.gout.service.impl;

import com.gout.dao.UserRepository;
import com.gout.dto.request.LoginRequest;
import com.gout.dto.request.RegisterRequest;
import com.gout.dto.response.TokenResponse;
import com.gout.entity.User;
import com.gout.global.exception.BusinessException;
import com.gout.global.exception.ErrorCode;
import com.gout.security.JwtTokenProvider;
import com.gout.security.RefreshTokenStore;
import com.gout.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenStore refreshTokenStore;

    @Override
    @Transactional
    public TokenResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .birthYear(request.getBirthYear())
                .build();

        User saved = userRepository.save(user);
        return issueTokens(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        if (user.getPassword() == null
                || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        return issueTokens(user);
    }

    /**
     * 리프레시 토큰 재발급 + 로테이션 (P1-8).
     *
     * 1) JWT 서명/만료 검증
     * 2) Redis 에 저장된 최신 토큰과 일치 검증 → 불일치/부재 시 INVALID_TOKEN (로그아웃·탈취·이전 토큰 재사용 차단)
     * 3) 새 토큰 발급 후 Redis SET 으로 덮어씀 → 방금 쓴 리프레시 토큰은 자동 폐기
     */
    @Override
    @Transactional(readOnly = true)
    public TokenResponse refresh(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        String userId = jwtTokenProvider.getUserId(refreshToken);

        if (!refreshTokenStore.isValid(userId, refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return issueTokens(user);
    }

    @Override
    public void logout(String userId) {
        if (userId == null || userId.isBlank()) {
            return;
        }
        refreshTokenStore.invalidate(userId);
    }

    private TokenResponse issueTokens(User user) {
        String access  = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail());
        String refresh = jwtTokenProvider.generateRefreshToken(user.getId());
        refreshTokenStore.save(user.getId(), refresh, jwtTokenProvider.getRefreshTokenExpirySeconds());
        return new TokenResponse(access, refresh);
    }
}

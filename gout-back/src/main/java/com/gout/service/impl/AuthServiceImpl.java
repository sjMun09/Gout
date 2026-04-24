package com.gout.service.impl;

import com.gout.dao.UserRepository;
import com.gout.dto.request.LoginRequest;
import com.gout.dto.request.RegisterRequest;
import com.gout.dto.response.TokenResponse;
import com.gout.entity.User;
import com.gout.global.exception.BusinessException;
import com.gout.global.exception.ErrorCode;
import com.gout.security.JwtTokenProvider;
import com.gout.security.JwtTokenProvider.ParsedToken;
import com.gout.security.RefreshTokenStore;
import com.gout.service.AuthService;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 회원가입 / 로그인 / 리프레시 토큰 재발급.
 *
 * <h3>P1-10 ~ P1-12 주요 변경</h3>
 * <ul>
 *   <li>refresh 재사용 탐지 — 사용된 jti 재제시 시 userId 전체 세션 invalidateAll + 보안 로그</li>
 *   <li>DELETED 사용자 refresh 차단</li>
 *   <li>login 타이밍 공격 방어 — 존재하지 않는 이메일에도 BCrypt 더미 비교 수행</li>
 *   <li>메시지 enumeration 방지 — 유저 없음 / 비번 불일치 동일 메시지(INVALID_CREDENTIALS)</li>
 *   <li>기존 10-cost 해시 자동 업그레이드 — 로그인 성공 시 12-cost 로 재해시 저장</li>
 *   <li>forceLogout(userId) — UserService 에서 비번 변경 / 탈퇴 시 호출</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    // BCrypt 12-cost 더미 해시. 유효 해시 길이/포맷이므로 matches 는 항상 false 반환.
    // 존재하지 않는 이메일에도 동일한 비용의 비교를 태워 응답 시간 차이를 최소화한다.
    // 평문 "gout-dummy-password" 를 strength=12 로 해시한 값.
    private static final String DUMMY_BCRYPT_HASH =
            "$2a$12$xjRjIC2vxCuRlOYZgC3fku5wUUG0HoKlfaQF3H2aHPzrIOfY8iGbu";

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
    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);

        // (1) enumeration 방어 — 유저가 없어도 BCrypt.matches 를 태워 응답 시간 균등화.
        if (user == null) {
            passwordEncoder.matches(request.getPassword(), DUMMY_BCRYPT_HASH);
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        // (2) 탈퇴 사용자는 유저 없음과 동일 메시지 — 상태 유출 방지.
        if (user.getStatus() == User.Status.DELETED) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        if (user.getPassword() == null
                || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        // (3) 기존 10-cost 해시 자동 업그레이드. BCryptPasswordEncoder(12) 로 bean 이 교체된 상황에서
        // 기존 10-cost 해시도 matches 는 통과하므로, 여기서 12-cost 해시로 재저장.
        maybeUpgradePasswordHash(user, request.getPassword());

        return issueTokens(user);
    }

    /**
     * 리프레시 토큰 재발급 + 재사용 탐지 (P1-10).
     */
    @Override
    @Transactional
    public TokenResponse refresh(String refreshToken) {
        ParsedToken parsed;
        try {
            parsed = jwtTokenProvider.parseRefresh(refreshToken);
        } catch (JwtException e) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        String userId = parsed.getUserId();
        String jti = parsed.getJti();

        // (1) 재사용 탐지 — 이미 used 집합에 있으면 탈취 의심, 전체 폐기.
        if (refreshTokenStore.isUsed(userId, jti)) {
            log.warn("REFRESH_REUSE_DETECTED userId={} jti={} — invalidating all sessions", userId, jti);
            refreshTokenStore.invalidateAll(userId);
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        // (2) 현재 유효한 jti 인지.
        if (!refreshTokenStore.isValid(userId, jti)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        // (3) 사용자 상태 — 탈퇴 사용자 차단 + 남은 세션 전부 폐기.
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getStatus() != User.Status.ACTIVE) {
            refreshTokenStore.invalidateAll(userId);
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        // (4) 로테이션 — 기존 jti 를 원자적으로 used 로 전환. 실패 시 동시 요청이 먼저 소비한 상태 → 재사용 간주.
        long ttl = jwtTokenProvider.getRefreshTokenExpirySeconds();
        if (!refreshTokenStore.tryMarkUsed(userId, jti, ttl)) {
            log.warn("REFRESH_CONCURRENT_REUSE userId={} jti={} — invalidating all sessions", userId, jti);
            refreshTokenStore.invalidateAll(userId);
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
        return issueTokens(user);
    }

    @Override
    public void logout(String userId) {
        if (userId == null || userId.isBlank()) {
            return;
        }
        refreshTokenStore.invalidateAll(userId);
    }

    /**
     * UserService 가 비밀번호 변경 / 탈퇴 시 호출. 모든 리프레시 세션 강제 종료.
     */
    public void forceLogout(String userId) {
        logout(userId);
    }

    // ======================================================================
    //  helpers
    // ======================================================================

    private TokenResponse issueTokens(User user) {
        String access = jwtTokenProvider.generateAccessToken(user.getId(), user.getRole().name());
        String refresh = jwtTokenProvider.generateRefreshToken(user.getId());
        // refresh 의 jti 는 parseRefresh 로 다시 뽑는다 — 구조 분해 중복이 싫다면 JwtTokenProvider 에
        // 생성 시 반환하는 builder 오버로드를 추가해도 됨. 현재는 1회 parsing 비용 허용.
        ParsedToken parsed = jwtTokenProvider.parseRefresh(refresh);
        refreshTokenStore.save(
                user.getId(), parsed.getJti(), jwtTokenProvider.getRefreshTokenExpirySeconds());
        return new TokenResponse(access, refresh);
    }

    private void maybeUpgradePasswordHash(User user, String rawPassword) {
        if (passwordEncoder.upgradeEncoding(user.getPassword())) {
            user.changePassword(passwordEncoder.encode(rawPassword));
        }
    }

    // UUID 생성이 필요한 경우 대비.
    @SuppressWarnings("unused")
    private static String newRequestId() {
        return UUID.randomUUID().toString();
    }
}

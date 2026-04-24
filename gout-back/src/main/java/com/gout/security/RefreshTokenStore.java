package com.gout.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 리프레시 토큰 Redis 저장소 (P1-8).
 *
 * 키 스킴: {@code refresh:{userId}} → 현재 유효한 리프레시 토큰 문자열.
 * 1 유저당 1 리프레시 — 로그인/재발급 시 SET 으로 덮어쓴다 → 이전 토큰 자동 폐기(로테이션).
 *
 * TTL 은 {@link JwtTokenProvider#getRefreshTokenExpirySeconds()} 와 동일하게 맞춘다.
 * JWT 자체 만료와 키 TTL 이 다르면 만료된 키가 서버에 남거나, 반대로 아직 유효한 JWT 가
 * Redis 에서는 이미 사라져 정상 재발급이 실패할 수 있다.
 *
 * TODO: 다기기 로그인을 허용하려면 키 스킴을 {@code refresh:{userId}:{deviceId}} 로 확장.
 *       현재는 1 유저 = 1 세션(최근 로그인만 유효) 정책.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenStore {

    private static final String KEY_PREFIX = "refresh:";

    private final StringRedisTemplate redisTemplate;

    /**
     * 리프레시 토큰을 저장. TTL 경과 후 자동 삭제된다.
     */
    public void save(String userId, String refreshToken, long ttlSeconds) {
        redisTemplate.opsForValue().set(key(userId), refreshToken, Duration.ofSeconds(ttlSeconds));
    }

    /**
     * 저장된 토큰과 제시된 토큰이 일치하는지 검증. 키가 없거나 값이 다르면 false.
     */
    public boolean isValid(String userId, String refreshToken) {
        if (userId == null || refreshToken == null) {
            return false;
        }
        String stored = redisTemplate.opsForValue().get(key(userId));
        return stored != null && stored.equals(refreshToken);
    }

    /**
     * 해당 유저의 리프레시 토큰을 삭제(로그아웃 / 강제 폐기).
     */
    public void invalidate(String userId) {
        redisTemplate.delete(key(userId));
    }

    private String key(String userId) {
        return KEY_PREFIX + userId;
    }
}

package com.gout.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * ADMIN access 토큰 jti 블랙리스트 (MED-004).
 *
 * <p>ADMIN 권한 access 토큰은 수명이 짧아도 (15min) 유출되면 탈취자가 남은 시간 동안 관리자
 * API 를 그대로 호출할 수 있다. refresh 토큰은 {@link RefreshTokenStore} 로 즉시 무효화
 * 가능하지만 access 는 stateless 서명 검증만 하므로 "logout 직후에도 기존 access 로 API 가 통과"
 * 하는 공백이 생긴다.
 *
 * <p>ADMIN 역할을 가진 access 토큰의 jti 를 Redis 에 저장해 logout 시점부터 access 만료 시점까지
 * 차단한다. 일반 유저 토큰은 저장/조회하지 않아 hot path 오버헤드를 admin 트래픽에만 한정.
 *
 * <p>네임스페이스: {@code gout:blacklist:admin:{jti}}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminTokenBlacklist {

    private static final String NS = "gout:blacklist:admin";
    private static final String VALUE = "1";

    private final StringRedisTemplate redisTemplate;

    /**
     * 해당 jti 를 블랙리스트에 추가. TTL 은 access 토큰의 남은 수명 (혹은 그 상한) 으로 잡는다.
     * Redis 장애 시 로그만 남기고 조용히 반환 — logout 자체가 500 으로 터지는 것을 막는다.
     */
    public void revoke(String jti, long ttlSeconds) {
        if (jti == null || jti.isBlank() || ttlSeconds <= 0) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(key(jti), VALUE, Duration.ofSeconds(ttlSeconds));
        } catch (Exception e) {
            log.warn("AdminTokenBlacklist.revoke failed for jti={}: {}", jti, e.toString());
        }
    }

    /**
     * 해당 jti 가 블랙리스트에 올라와 있는지. Redis 장애 시 false (fail-open) — 장애가 전체
     * admin 접근 차단으로 번지는 것을 막는다. 장애는 별도 모니터링으로 감지.
     */
    public boolean isRevoked(String jti) {
        if (jti == null || jti.isBlank()) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key(jti)));
        } catch (Exception e) {
            log.warn("AdminTokenBlacklist.isRevoked failed for jti={}: {}", jti, e.toString());
            return false;
        }
    }

    private String key(String jti) {
        return NS + ":" + jti;
    }
}

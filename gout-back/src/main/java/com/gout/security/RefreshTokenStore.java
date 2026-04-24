package com.gout.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;

/**
 * 리프레시 토큰 Redis 저장소 (P1-10 재설계).
 *
 * <h3>키 스킴</h3>
 * <pre>
 * gout:refresh:v1:{userId}:{jti}        → "valid" (TTL = refresh 만료)
 * gout:refresh:v1:used:{userId}:{jti}   → "used"  (TTL = refresh 만료)
 * </pre>
 *
 * <h3>목적</h3>
 * <ul>
 *   <li>사용된 jti 를 별도 키(used)로 옮겨 재사용 탐지</li>
 *   <li>(userId, jti) 복합 키 — 멀티 디바이스 로그인 지원 (기존 1유저=1세션 정책 확장)</li>
 *   <li>invalidateAll(userId) — SCAN 으로 user 관련 전체 키 삭제 (로그아웃/탈퇴/비번변경)</li>
 * </ul>
 *
 * <h3>네임스페이스 변경 영향</h3>
 * 구 키 {@code refresh:{userId}} 는 더 이상 쓰이지 않는다. 배포 시 정리 스크립트 필요.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenStore {

    private static final String NS = "gout:refresh:v1";
    private static final String VALUE_VALID = "valid";
    private static final String VALUE_USED = "used";

    private final StringRedisTemplate redisTemplate;

    /** 새로 발급된 리프레시 토큰의 jti 를 유효 상태로 저장. */
    public void save(String userId, String jti, long ttlSeconds) {
        requireNonBlank(userId, "userId");
        requireNonBlank(jti, "jti");
        if (ttlSeconds <= 0) {
            throw new IllegalArgumentException("ttlSeconds must be > 0");
        }
        redisTemplate.opsForValue().set(validKey(userId, jti), VALUE_VALID, Duration.ofSeconds(ttlSeconds));
    }

    /** 해당 jti 가 현재 유효한 리프레시 세션에 속하는지. */
    public boolean isValid(String userId, String jti) {
        if (userId == null || jti == null) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(validKey(userId, jti)));
    }

    /** 해당 jti 가 이미 사용(=로테이션된) 기록이 있는지 — 재사용 탐지용. */
    public boolean isUsed(String userId, String jti) {
        if (userId == null || jti == null) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(usedKey(userId, jti)));
    }

    /**
     * valid → used 로 전환. 토큰 TTL 이 남아있는 동안만 used 마킹이 유지된다.
     * valid 키가 없으면 아무 일도 하지 않는다(이미 로그아웃 등 만료 상태).
     */
    public void markUsed(String userId, String jti, long ttlSeconds) {
        requireNonBlank(userId, "userId");
        requireNonBlank(jti, "jti");
        redisTemplate.delete(validKey(userId, jti));
        redisTemplate.opsForValue().set(
                usedKey(userId, jti), VALUE_USED, Duration.ofSeconds(Math.max(ttlSeconds, 1)));
    }

    /**
     * 해당 userId 의 refresh 관련 키(valid + used)를 전부 삭제.
     *
     * <p>호출 시점:
     * <ul>
     *   <li>로그아웃 ({@link AuthServiceImpl#logout})</li>
     *   <li>탈퇴 / 비밀번호 변경</li>
     *   <li>refresh 재사용 탐지 → 전체 세션 강제 종료</li>
     * </ul>
     */
    public void invalidateAll(String userId) {
        if (userId == null || userId.isBlank()) {
            return;
        }
        String pattern = NS + ":*" + userId + ":*";
        // SCAN 으로 일치 키 수집 후 배치 DEL. KEYS 는 프로덕션에서 블록 이슈 → 사용 금지.
        Set<String> matched = redisTemplate.execute((RedisConnection conn) -> {
            java.util.HashSet<String> keys = new java.util.HashSet<>();
            ScanOptions options = ScanOptions.scanOptions().match(pattern).count(200).build();
            try (Cursor<byte[]> cursor = conn.keyCommands().scan(options)) {
                while (cursor.hasNext()) {
                    keys.add(new String(cursor.next(), StandardCharsets.UTF_8));
                }
            }
            return keys;
        });
        if (matched != null && !matched.isEmpty()) {
            redisTemplate.delete(matched);
            log.info("invalidateAll userId={} deletedKeys={}", userId, matched.size());
        }
    }

    /** 기존 단일-세션 API 호환용 — 전부 삭제와 동일. */
    public void invalidate(String userId) {
        invalidateAll(userId);
    }

    private String validKey(String userId, String jti) {
        return NS + ":" + userId + ":" + jti;
    }

    private String usedKey(String userId, String jti) {
        return NS + ":used:" + userId + ":" + jti;
    }

    private static void requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}

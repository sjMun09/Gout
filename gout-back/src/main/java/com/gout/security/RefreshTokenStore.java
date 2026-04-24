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
     * 원자적으로 jti 를 used 로 전환 (P2-43: TOCTOU 제거).
     *
     * <p>Redis {@code SET NX EX} 로 {@code usedKey} 를 생성한 뒤, 성공한 호출자만 {@code validKey} 를 제거한다.
     * 동시 요청 2건이 같은 jti 로 경쟁해도 setIfAbsent 가 단 하나만 {@code true} 를 반환하므로
     * 정확히 한 호출자만 토큰을 소비할 수 있다. 경쟁에 진 쪽은 호출자가 invalidateAll 로 전체 세션을 폐기해야 한다.
     *
     * <p>과거 {@code isUsed() → markUsed()} 2-스텝 구현은 두 호출 사이에 TOCTOU 윈도우가 존재해
     * legit/attacker 가 같은 jti 로 동시에 /refresh 를 호출하면 둘 다 통과할 수 있었다(RFC 8725 §4.12 미달).
     *
     * <p>주의: {@code validKey} 존재 여부는 검사하지 않는다. 호출자는 이 메서드 이전에
     * {@link #isValid(String, String)} 로 현재 유효한 세션인지 확인해야 한다.
     *
     * @return {@code true} 이면 이번 호출이 처음으로 이 jti 를 소비. {@code false} 면 이미 used —
     *         재사용 또는 동시 경쟁 패배 → 호출자는 invalidateAll 로 대응.
     */
    public boolean tryMarkUsed(String userId, String jti, long ttlSeconds) {
        requireNonBlank(userId, "userId");
        requireNonBlank(jti, "jti");
        Boolean set = redisTemplate.opsForValue().setIfAbsent(
                usedKey(userId, jti), VALUE_USED, Duration.ofSeconds(Math.max(ttlSeconds, 1)));
        if (!Boolean.TRUE.equals(set)) {
            return false;
        }
        redisTemplate.delete(validKey(userId, jti));
        return true;
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
     *
     * <p>P2-46: 과거 단일 패턴 {@code NS + ":*" + userId + ":*"} 는 valid 와 used 키를 한 번에 걸쳤으나,
     * UUID 외 형식이 들어오거나 키 스키마가 확장되면 false-positive 매칭이 발생할 수 있었다.
     * 이제 두 패턴을 명시적으로 나눠서 각각 SCAN 한다 — 스키마 변화에 내성.
     */
    public void invalidateAll(String userId) {
        if (userId == null || userId.isBlank()) {
            return;
        }
        long deleted = 0L;
        deleted += scanAndDelete(NS + ":" + userId + ":*");       // valid 키
        deleted += scanAndDelete(NS + ":used:" + userId + ":*");  // used 키
        if (deleted > 0) {
            log.info("REFRESH_INVALIDATE_ALL userId={} deleted={}", userId, deleted);
        }
    }

    /**
     * 주어진 패턴에 매치되는 키를 SCAN 으로 모아 배치 DEL. KEYS 는 프로덕션에서 블록 이슈 → 사용 금지.
     * @return 실제 삭제된 키 개수.
     */
    private long scanAndDelete(String pattern) {
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
        if (matched == null || matched.isEmpty()) {
            return 0L;
        }
        Long count = redisTemplate.delete(matched);
        return count != null ? count : 0L;
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

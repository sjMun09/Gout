package com.gout.security;

import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentMap;

/**
 * Bucket4j 기반 인메모리 레이트 리미터.
 *
 * - 키별로 독립적인 Token Bucket 유지.
 * - Caffeine 으로 1시간 미접근 키 자동 만료 → 메모리 누수 방지.
 * - {@link Bandwidth} 는 호출자가 직접 지정 (로그인 5/min, 좋아요 30/min 등).
 *
 * <p>TODO: Bucket4j 상태는 현재 <b>in-memory only</b>.
 *    멀티 인스턴스 배포 시 각 인스턴스가 독립 버킷을 가져 실제 제한이 N배로 느슨해짐.
 *    운영 전에 Redis(jcache-redis) 또는 Hazelcast 백엔드로 전환 필요 — 별도 PR.
 */
@Service
public class RateLimiterService {

    /** 로그인: IP 당 1분 5회 (brute-force 방어). */
    public static final Bandwidth LOGIN_BANDWIDTH = Bandwidth.classic(
            5,
            Refill.intervally(5, Duration.ofMinutes(1))
    );

    /** 좋아요: 사용자 당 1분 30회 (spam 방어). */
    public static final Bandwidth LIKE_BANDWIDTH = Bandwidth.classic(
            30,
            Refill.intervally(30, Duration.ofMinutes(1))
    );

    /**
     * 키별 버킷 저장소. Caffeine 의 asMap() 로 ConcurrentMap 래핑.
     * expireAfterAccess(1h) — 1시간 동안 조회/갱신이 없으면 자동 제거.
     */
    private final ConcurrentMap<String, Bucket> buckets =
            Caffeine.newBuilder()
                    .expireAfterAccess(Duration.ofHours(1))
                    .<String, Bucket>build()
                    .asMap();

    /**
     * 토큰 1개 소비를 시도. 성공 시 true, 버킷이 비었으면 false.
     *
     * @param key       버킷 키 (IP / userId 등)
     * @param bandwidth 해당 키에 적용할 대역폭 — 최초 1회만 사용됨 (이후 재사용 시 기존 버킷 유지)
     */
    public boolean tryConsume(String key, Bandwidth bandwidth) {
        Bucket bucket = buckets.computeIfAbsent(key, k -> newBucket(bandwidth));
        return bucket.tryConsume(1);
    }

    /**
     * 테스트/운영 이슈 복구용. 모든 버킷을 비운다.
     * 프로덕션 코드에서 호출하지 말 것.
     */
    public void reset() {
        buckets.clear();
    }

    private Bucket newBucket(Bandwidth bandwidth) {
        return Bucket.builder().addLimit(bandwidth).build();
    }
}

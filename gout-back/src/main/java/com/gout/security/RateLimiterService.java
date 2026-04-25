package com.gout.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Bucket4j 기반 레이트 리미터 — Lettuce Redis 프록시 백엔드 (HIGH-004).
 *
 * <p>이전 구현(Caffeine in-memory) 은 멀티 인스턴스 간 버킷이 공유되지 않아 실제 제한이
 * 인스턴스 수만큼 느슨해졌다. Redis 백엔드로 전환해 atomic token bucket 을 공유한다.
 *
 * <p>리프레시 토큰 저장소(P1-8) 의 Spring Data Redis 설정을 그대로 재사용해 새 인프라
 * 의존성은 없다. LettuceBasedProxyManager 는 Spring 이 들고 있는 pool 과 별개로
 * 전용 long-lived connection 을 1개 유지한다 (bucket4j 요구사항).
 *
 * <p><b>Fail-open</b>: Redis 장애 시 {@link #tryConsume} 가 true(허용) 를 반환한다.
 * 가용성을 엄격한 제한보다 우선한다 — 로그인이 Redis 장애로 500 을 뱉는 것보다 잠깐
 * 느슨해지는 편이 낫다. 장애는 로그/메트릭에서 별도로 감지한다.
 */
@Slf4j
@Service
public class RateLimiterService {

    private static final String KEY_PREFIX = "ratelimit:";

    /** 로그인: IP 당 1분 5회 (brute-force 방어). */
    public static final Bandwidth LOGIN_BANDWIDTH = Bandwidth.classic(
            5,
            Refill.intervally(5, Duration.ofMinutes(1))
    );

    /**
     * 회원가입: IP 당 10분 10회 (CPU DoS + 이메일 열거 + 계정 폭탄 방어).
     *
     * <p>register 는 BCrypt cost-12 해시 연산을 포함하므로 login 보다 CPU 비용이 크다.
     * 정상 사용 패턴(한 번에 여러 계정 생성)을 고려해 윈도우를 10분으로 넓혔다.
     */
    public static final Bandwidth REGISTER_BANDWIDTH = Bandwidth.classic(
            10,
            Refill.intervally(10, Duration.ofMinutes(10))
    );

    /** 좋아요: 사용자 당 1분 30회 (spam 방어). */
    public static final Bandwidth LIKE_BANDWIDTH = Bandwidth.classic(
            30,
            Refill.intervally(30, Duration.ofMinutes(1))
    );

    private final RedisClient redisClient;
    private final StatefulRedisConnection<String, byte[]> connection;
    private final LettuceBasedProxyManager<String> proxyManager;

    public RateLimiterService(LettuceConnectionFactory factory) {
        RedisURI uri = RedisURI.Builder
                .redis(factory.getHostName(), factory.getPort())
                .withTimeout(Duration.ofSeconds(1))
                .build();
        String password = factory.getPassword();
        if (password != null && !password.isEmpty()) {
            uri.setPassword(password.toCharArray());
        }
        this.redisClient = RedisClient.create(uri);
        RedisCodec<String, byte[]> codec = RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE);
        this.connection = redisClient.connect(codec);
        this.proxyManager = LettuceBasedProxyManager.builderFor(connection)
                .withExpirationStrategy(
                        ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(
                                Duration.ofHours(1)))
                .build();
        log.info("RateLimiterService initialized with Redis backend at {}:{}",
                factory.getHostName(), factory.getPort());
    }

    /**
     * 토큰 1개 소비를 시도. 성공 시 true, 버킷이 비었으면 false.
     *
     * <p>Redis 장애 시 true (fail-open). Redis 에러는 log.warn 으로 남기되 상위에 전파하지 않는다.
     */
    public boolean tryConsume(String key, Bandwidth bandwidth) {
        try {
            BucketConfiguration config = BucketConfiguration.builder()
                    .addLimit(bandwidth)
                    .build();
            BucketProxy bucket = proxyManager.builder().build(KEY_PREFIX + key, () -> config);
            return bucket.tryConsume(1);
        } catch (Exception e) {
            log.warn("Rate limiter Redis failure, fail-open for key={}: {}", key, e.toString());
            return true;
        }
    }

    /**
     * 테스트/운영 이슈 복구용. {@value #KEY_PREFIX} 로 시작하는 키만 삭제한다 (리프레시 토큰 등
     * 다른 네임스페이스의 키는 보존). 프로덕션 코드에서 호출하지 말 것.
     */
    public void reset() {
        try {
            var sync = connection.sync();
            var keys = sync.keys(KEY_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                sync.del(keys.toArray(new String[0]));
            }
        } catch (Exception e) {
            log.warn("Rate limiter reset failed: {}", e.toString());
        }
    }

    @PreDestroy
    void shutdown() {
        try {
            connection.close();
        } catch (Exception ignored) {
        }
        try {
            redisClient.shutdown();
        } catch (Exception ignored) {
        }
    }
}

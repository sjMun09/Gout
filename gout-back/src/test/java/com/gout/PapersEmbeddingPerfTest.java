package com.gout;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Random;
import java.util.StringJoiner;

/**
 * papers.embedding HNSW 인덱스 성능 측정용 테스트.
 *
 * <p>실행 방법:
 * <pre>
 *   ./gradlew test --tests com.gout.PapersEmbeddingPerfTest -Dperf.enabled=true
 * </pre>
 *
 * <p>기본적으로 @Disabled 로 스킵한다. 환경마다 레이턴시 편차가 크기 때문에
 * 강한 assertion 을 걸지 않고 stdout 으로 결과를 출력하는 형태로 설계했다.
 *
 * <p>Flyway 를 타지 않고 1,536차원 pgvector + HNSW 인덱스 빌드/쿼리만 직접 검증한다
 * (Spring context 기동 비용 없음). 전체 마이그레이션의 실제 적용은 Flyway 통합 테스트
 * ({@link IntegrationTestBase} 경유) 에서 간접 검증된다.
 */
@Disabled("perf-only; 로컬에서 수동 실행 (-Dperf.enabled=true 로 전환하거나 @Disabled 제거)")
class PapersEmbeddingPerfTest {

    private static final int DIM = 1536;
    private static final int SEED_ROWS = 1_000;
    private static final int QUERY_ITERATIONS = 100;

    @Test
    @DisplayName("HNSW 인덱스 기반 top-10 cosine-neighbor 쿼리 레이턴시 측정")
    void hnsw_query_latency() throws Exception {
        // pgvector + HNSW 지원이 필요하므로 pgvector/pgvector:pg17 이미지를 사용.
        try (PostgreSQLContainer<?> pg = new PostgreSQLContainer<>(
                DockerImageName.parse("pgvector/pgvector:pg17")
                        .asCompatibleSubstituteFor("postgres"))
                .withDatabaseName("perf")
                .withUsername("perf")
                .withPassword("perf")) {
            pg.start();

            try (Connection conn = DriverManager.getConnection(
                    pg.getJdbcUrl(), pg.getUsername(), pg.getPassword())) {

                // 스키마 + 인덱스 세팅 (V25 와 동일한 파라미터)
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("CREATE EXTENSION IF NOT EXISTS vector");
                    stmt.execute("""
                            CREATE TABLE papers (
                                id TEXT PRIMARY KEY,
                                embedding vector(1536)
                            )
                            """);
                }

                // 1,000 건 랜덤 임베딩 seed
                long seedStart = System.nanoTime();
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO papers(id, embedding) VALUES (?, ?::vector)")) {
                    Random rnd = new Random(42);
                    for (int i = 0; i < SEED_ROWS; i++) {
                        ps.setString(1, "paper-" + i);
                        ps.setString(2, randomVectorLiteral(rnd));
                        ps.addBatch();
                        if (i % 100 == 99) ps.executeBatch();
                    }
                    ps.executeBatch();
                }
                long seedMs = (System.nanoTime() - seedStart) / 1_000_000L;

                // HNSW 인덱스 빌드
                long buildStart = System.nanoTime();
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("""
                            CREATE INDEX idx_papers_embedding
                                ON papers USING hnsw (embedding vector_cosine_ops)
                                WITH (m = 16, ef_construction = 64)
                            """);
                }
                long buildMs = (System.nanoTime() - buildStart) / 1_000_000L;

                // 쿼리 벤치 — 100 회, 매번 랜덤 쿼리 벡터
                Random rnd = new Random(7);
                long[] latencies = new long[QUERY_ITERATIONS];
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT id FROM papers ORDER BY embedding <=> ?::vector LIMIT 10")) {
                    for (int i = 0; i < QUERY_ITERATIONS; i++) {
                        ps.setString(1, randomVectorLiteral(rnd));
                        long start = System.nanoTime();
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) { rs.getString(1); }
                        }
                        latencies[i] = (System.nanoTime() - start) / 1_000_000L;
                    }
                }

                Arrays.sort(latencies);
                long p50 = latencies[latencies.length / 2];
                long p95 = latencies[(int) (latencies.length * 0.95) - 1];
                long p99 = latencies[(int) (latencies.length * 0.99) - 1];
                double mean = Arrays.stream(latencies).average().orElse(0);

                System.out.printf("""
                        [PapersEmbeddingPerfTest]
                          rows        = %d
                          seed time   = %d ms
                          index build = %d ms (HNSW m=16, ef_construction=64)
                          iterations  = %d
                          mean        = %.2f ms
                          p50 / p95   = %d / %d ms
                          p99         = %d ms
                        %n""",
                        SEED_ROWS, seedMs, buildMs,
                        QUERY_ITERATIONS, mean, p50, p95, p99);

                // 환경 의존이 크므로 강한 assertion 은 피한다.
                // 명백한 회귀(1초 이상) 만 방지한다.
                if (p95 > 1_000) {
                    throw new AssertionError("p95 latency too high: " + p95 + " ms");
                }
            }
        }
    }

    private static String randomVectorLiteral(Random rnd) {
        StringJoiner sj = new StringJoiner(",", "[", "]");
        for (int i = 0; i < DIM; i++) {
            // [-1, 1] 범위 정규화는 생략 — 상대 순위만 비교하므로 스케일 무관.
            sj.add(Float.toString((rnd.nextFloat() * 2f) - 1f));
        }
        return sj.toString();
    }
}

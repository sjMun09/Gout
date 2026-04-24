package com.gout;

import com.gout.security.RateLimiterService;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.web.FilterChainProxy;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

/**
 * Testcontainers 기반 통합 테스트 공용 베이스.
 *
 * PostGIS + pgvector 확장이 모두 필요해 로컬에서 사전에 빌드된 이미지
 * (gout-test-postgis-pgvector:local — docker/postgres.Dockerfile 기반)를 사용한다.
 *
 * 로컬 실행 전:
 *   docker build -t gout-test-postgis-pgvector:local -f docker/postgres.Dockerfile docker/
 *
 * CI 에서는 test 실행 직전에 동일 명령으로 빌드된다.
 * Flyway V1: uuid-ossp / postgis / vector / pg_trgm 설치.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    /**
     * JVM-wide 싱글턴 컨테이너. 모든 테스트 클래스가 같은 컨테이너를 재사용한다.
     * static 초기화 블록에서 한 번만 start() — JUnit @Testcontainers 라이프사이클에 맡기면
     * 각 테스트 클래스 종료 시 컨테이너가 정리되고 다음 클래스는 새 컨테이너를 띄우는데,
     * Spring context 캐시는 첫 컨테이너의 JDBC URL 을 계속 재사용 → 끊어진 포트로 접속 실패.
     */
    @ServiceConnection
    @SuppressWarnings("resource")
    protected static final PostgreSQLContainer<?> POSTGRES;

    /**
     * Redis 싱글턴 컨테이너 — 리프레시 토큰 저장소(P1-8) 검증용.
     * 같은 Spring context 캐시를 재사용하므로 Postgres 와 동일하게 JVM-wide 로 유지.
     */
    @SuppressWarnings("resource")
    protected static final GenericContainer<?> REDIS;

    static {
        POSTGRES = new PostgreSQLContainer<>(
                DockerImageName.parse("gout-test-postgis-pgvector:local")
                        .asCompatibleSubstituteFor("postgres"))
                .withDatabaseName("gout_test")
                .withUsername("test")
                .withPassword("test");
        POSTGRES.start();

        REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379);
        REDIS.start();
    }

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @Autowired
    protected WebApplicationContext context;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected FilterChainProxy springSecurityFilterChain;

    @Autowired
    protected RateLimiterService rateLimiterService;

    protected MockMvc mockMvc;

    @BeforeEach
    void setUpMockMvc() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity(springSecurityFilterChain))
                .build();
        // P1-9: 테스트 간 레이트 리밋 상태가 남아 있으면 (특히 로그인 5/min) 기존 루프형 테스트가
        // 429 로 떨어지므로, 각 테스트 시작 시점에 버킷을 리셋.
        rateLimiterService.reset();
    }

    // ============== Helpers ==============

    protected String toJson(Object body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    /**
     * 회원가입. 성공 시 ApiResponse<TokenResponse> 반환 JSON.
     */
    protected JsonNode register(String email, String password, String nickname) throws Exception {
        Map<String, Object> body = Map.of(
                "email", email,
                "password", password,
                "nickname", nickname
        );
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(body)))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    /**
     * 로그인. 성공 시 accessToken 반환.
     */
    protected String login(String email, String password) throws Exception {
        Map<String, Object> body = Map.of(
                "email", email,
                "password", password
        );
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(body)))
                .andReturn();
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("accessToken").asText();
    }

    /**
     * register + login 콤보. accessToken 반환.
     */
    protected String registerAndLogin(String email, String password, String nickname) throws Exception {
        JsonNode reg = register(email, password, nickname);
        // 응답에 accessToken이 이미 담겨있으면 그대로 반환, 없으면 login 호출.
        String token = reg.path("data").path("accessToken").asText(null);
        if (token != null && !token.isEmpty()) {
            return token;
        }
        return login(email, password);
    }

    protected String authHeader(String token) {
        return "Bearer " + token;
    }
}

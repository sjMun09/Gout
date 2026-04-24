package com.gout;

import com.gout.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 관리자 패널 통합 테스트.
 *
 * 설계:
 *  - register() 는 gender_type 버그로 실패 → JdbcTemplate 직접 INSERT 로 유저 생성.
 *  - @PreAuthorize 가 ROLE_ADMIN 을 요구하므로 CustomUserDetailsService 가 DB 에서
 *    role 을 조회해 권한 매핑하는 경로를 타야 실제 인가 흐름 검증이 된다.
 *  - users.status 컬럼은 Agent-H V23 머지 전이라 없을 수 있음.
 *    suspend 테스트 전에 test 시작 시점에 ADD COLUMN IF NOT EXISTS 로 보정.
 */
class AdminIntegrationTest extends IntegrationTestBase {

    @Autowired
    JwtTokenProvider jwtTokenProvider;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private String adminId;
    private String adminToken;
    private String normalUserId;
    private String normalUserToken;
    private String targetUserId;

    @BeforeEach
    void seedUsers() {
        // Agent-H V23 이 들어오기 전이면 users.status 컬럼 없음 → 보정
        // (버전 마이그레이션은 아님. 테스트 전용 DDL 보정. 실제 V23 이 나중에 들어와도 충돌 없음.)
        jdbcTemplate.execute(
                "ALTER TABLE users ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'");

        // 기존 테스트 유저 제거 (같은 이메일로 여러 테스트 실행 시 충돌 방지)
        jdbcTemplate.update("DELETE FROM users WHERE email LIKE ?", "admintest-%@gout.test");

        adminId = insertUser("admintest-admin-" + UUID.randomUUID() + "@gout.test", "관리자", "ADMIN");
        normalUserId = insertUser("admintest-normal-" + UUID.randomUUID() + "@gout.test", "일반유저", "USER");
        targetUserId = insertUser("admintest-target-" + UUID.randomUUID() + "@gout.test", "정지대상", "USER");

        adminToken = jwtTokenProvider.generateAccessToken(adminId, "admintest-admin@gout.test");
        normalUserToken = jwtTokenProvider.generateAccessToken(normalUserId, "admintest-normal@gout.test");
    }

    /**
     * JdbcTemplate 로 직접 INSERT. register() 의 gender_type enum 버그를 우회.
     * role 은 PostgreSQL ENUM(user_role) 이라 CAST 필수.
     */
    private String insertUser(String email, String nickname, String role) {
        String id = UUID.randomUUID().toString();
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password, nickname, role, status) "
                        + "VALUES (?, ?, ?, ?, CAST(? AS user_role), 'ACTIVE')",
                id, email, "$2a$10$dummyhash", nickname, role
        );
        return id;
    }

    @Test
    @DisplayName("비-ADMIN 토큰으로 /api/admin/users 호출 시 403")
    void non_admin_gets_forbidden() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, authHeader(normalUserToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("토큰 없이 /api/admin/users 호출 시 401 또는 403")
    void anonymous_gets_unauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("ADMIN 토큰으로 /api/admin/users 호출 시 200 + 유저 목록 반환")
    void admin_can_list_users() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, authHeader(adminToken))
                        .param("page", "0")
                        .param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("ADMIN 이 유저 정지 → status=SUSPENDED, 해제 → status=ACTIVE")
    void admin_suspend_and_unsuspend_flow() throws Exception {
        // suspend
        mockMvc.perform(post("/api/admin/users/" + targetUserId + "/suspend")
                        .header(HttpHeaders.AUTHORIZATION, authHeader(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        String suspendedStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM users WHERE id = ?", String.class, targetUserId);
        assertEquals("SUSPENDED", suspendedStatus);

        // unsuspend
        mockMvc.perform(post("/api/admin/users/" + targetUserId + "/unsuspend")
                        .header(HttpHeaders.AUTHORIZATION, authHeader(adminToken)))
                .andExpect(status().isOk());

        String activeStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM users WHERE id = ?", String.class, targetUserId);
        assertEquals("ACTIVE", activeStatus);
    }

    @Test
    @DisplayName("비-ADMIN 이 suspend 시도 → 403, 대상 유저 status 변동 없음")
    void non_admin_cannot_suspend() throws Exception {
        mockMvc.perform(post("/api/admin/users/" + targetUserId + "/suspend")
                        .header(HttpHeaders.AUTHORIZATION, authHeader(normalUserToken)))
                .andExpect(status().isForbidden());

        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM users WHERE id = ?", String.class, targetUserId);
        assertNotEquals("SUSPENDED", status);
    }

    @Test
    @DisplayName("ADMIN 이 promote → role=ADMIN 으로 갱신")
    void admin_can_promote_user() throws Exception {
        mockMvc.perform(post("/api/admin/users/" + targetUserId + "/promote")
                        .header(HttpHeaders.AUTHORIZATION, authHeader(adminToken)))
                .andExpect(status().isOk());

        String role = jdbcTemplate.queryForObject(
                "SELECT role::text FROM users WHERE id = ?", String.class, targetUserId);
        assertEquals("ADMIN", role);
    }
}

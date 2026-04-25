package com.gout;

import com.gout.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MED-004 — ADMIN access 토큰 jti 블랙리스트 통합 테스트.
 *
 * <p>시나리오:
 *  1. ADMIN 유저 INSERT + 같은 유저로 access 토큰 발급 (roles=["ADMIN"] 포함).
 *  2. 그 access 로 /api/admin/users 호출 → 200 (정상 경로 확인).
 *  3. 같은 access 로 /api/auth/logout 호출 → AdminTokenBlacklist 에 jti 등록됨.
 *  4. 같은 access 로 다시 /api/admin/users 호출 → 401 (블랙리스트 차단).
 *
 * <p>일반 유저는 블랙리스트에 등록되지 않음을 함께 확인 (hot path 오버헤드 없음).
 */
class AdminLogoutBlacklistIntegrationTest extends IntegrationTestBase {

    @Autowired
    JwtTokenProvider jwtTokenProvider;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private String insertUser(String email, String nickname, String role) {
        String id = UUID.randomUUID().toString();
        jdbcTemplate.execute(
                "ALTER TABLE users ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'");
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password, nickname, role, status) "
                        + "VALUES (?, ?, ?, ?, CAST(? AS user_role), 'ACTIVE')",
                id, email, "$2a$10$dummyhash", nickname, role
        );
        return id;
    }

    @Test
    @DisplayName("ADMIN logout 후 같은 access 로 /api/admin/users → 401 블랙리스트 차단")
    void admin_access_blocked_after_logout() throws Exception {
        String adminId = insertUser(
                "jti-blacklist-admin-" + UUID.randomUUID() + "@gout.test", "jti관리자", "ADMIN");
        String token = jwtTokenProvider.generateAccessToken(adminId, "ADMIN");
        String auth = "Bearer " + token;

        // 1. logout 전 — 정상 접근
        mockMvc.perform(get("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .param("page", "0").param("size", "10"))
                .andExpect(status().isOk());

        // 2. 같은 access 로 logout → jti 가 블랙리스트에 오름
        mockMvc.perform(post("/api/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk());

        // 3. 다시 같은 access 로 admin API 호출 → 401
        mockMvc.perform(get("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .param("page", "0").param("size", "10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("일반 유저 logout 은 access 를 블랙리스트에 올리지 않아 같은 토큰 재사용 가능 (hot path 영향 없음)")
    void normal_user_access_not_blacklisted_after_logout() throws Exception {
        String userId = insertUser(
                "jti-blacklist-user-" + UUID.randomUUID() + "@gout.test", "jti일반", "USER");
        String token = jwtTokenProvider.generateAccessToken(userId, "USER");
        String auth = "Bearer " + token;

        mockMvc.perform(post("/api/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk());

        // 일반 유저 로그아웃은 access 를 폐기하지 않는다 (refresh 만 폐기). 같은 access 로 개인 API 접근 가능.
        // /api/health/uric-acid-logs 는 authenticated() 로 보호되므로 블랙리스트 미적용이면 2xx.
        mockMvc.perform(get("/api/health/uric-acid-logs")
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().is2xxSuccessful());
    }
}

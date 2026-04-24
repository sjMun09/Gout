package com.gout;

import com.gout.security.JwtTokenProvider;
import com.gout.security.JwtTokenProvider.ParsedToken;
import com.gout.security.RefreshTokenStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CRIT-001: SUSPENDED 상태 유저의 로그인 및 리프레시 토큰 재발급 차단 검증.
 *
 * 시나리오:
 *  1. 유저 등록 → 리프레시 토큰 획득
 *  2. 관리자가 해당 유저를 SUSPENDED 로 직접 UPDATE
 *  3. 해당 유저로 로그인 시도 → 401
 *  4. 정지 전 발급된 리프레시 토큰으로 재발급 시도 → 401
 */
class SuspendedUserAccessIntegrationTest extends IntegrationTestBase {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private RefreshTokenStore refreshTokenStore;

    private String suspendedEmail;
    private String suspendedPassword;
    private String suspendedUserId;
    private String refreshTokenBeforeSuspend;

    @BeforeEach
    void setUpSuspendedUser() throws Exception {
        suspendedEmail = "suspended-" + UUID.randomUUID() + "@gout.test";
        suspendedPassword = "Password123!";

        // 회원가입 후 리프레시 토큰 획득
        JsonNode reg = register(suspendedEmail, suspendedPassword, "정지대상유저");
        refreshTokenBeforeSuspend = reg.path("data").path("refreshToken").asText();
        String accessToken = reg.path("data").path("accessToken").asText();

        // userId 추출 (access token 파싱)
        ParsedToken parsed = jwtTokenProvider.parseRefresh(refreshTokenBeforeSuspend);
        suspendedUserId = parsed.getUserId();

        // users.status 컬럼이 VARCHAR 인 경우 보정 (테스트 환경용)
        jdbcTemplate.execute(
                "ALTER TABLE users ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'");

        // 관리자가 정지 처리 (native UPDATE — AdminServiceImpl 과 동일한 방식)
        jdbcTemplate.update(
                "UPDATE users SET status = 'SUSPENDED', updated_at = NOW() WHERE id = ?",
                suspendedUserId);
    }

    @Test
    @DisplayName("SUSPENDED 유저 로그인 시도 → 401 반환")
    void suspended_user_cannot_login() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of(
                                "email", suspendedEmail,
                                "password", suspendedPassword
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("SUSPENDED 유저의 기존 리프레시 토큰으로 재발급 시도 → 401 반환")
    void suspended_user_refresh_token_rejected() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("refreshToken", refreshTokenBeforeSuspend))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }
}

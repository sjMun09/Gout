package com.gout;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P1-9: 로그인 엔드포인트 IP 기반 레이트 리밋 (5 requests/min/IP).
 *
 * given-when-then BDD 스타일.
 */
class LoginRateLimitIntegrationTest extends IntegrationTestBase {

    private static final String ATTACKER_IP = "10.0.0.99";
    private static final String OTHER_IP = "10.0.0.100";

    @Test
    @DisplayName("로그인 5회 이하 실패는 401 만 반환 (429 아님)")
    void login_under_limit_succeeds() throws Exception {
        // given: 존재하지 않는 계정 (항상 401 응답)
        Map<String, Object> badCreds = Map.of(
                "email", "nobody@gout.test",
                "password", "wrong-password"
        );

        // when / then: 4회 시도 — 모두 401, 429 아님
        for (int i = 0; i < 4; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .with(req -> { req.setRemoteAddr(ATTACKER_IP); return req; })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(badCreds)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    @DisplayName("같은 IP에서 6번째 로그인 시도 시 429 + Retry-After: 60")
    void login_exceeds_limit_returns429() throws Exception {
        // given: 잘못된 자격 증명 + 동일 IP
        Map<String, Object> badCreds = Map.of(
                "email", "nobody@gout.test",
                "password", "wrong-password"
        );

        // when: 5회까지 소비 → 버킷 empty
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .with(req -> { req.setRemoteAddr(ATTACKER_IP); return req; })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(badCreds)))
                    .andExpect(status().isUnauthorized());
        }

        // then: 6번째는 429 + Retry-After 헤더
        mockMvc.perform(post("/api/auth/login")
                        .with(req -> { req.setRemoteAddr(ATTACKER_IP); return req; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(badCreds)))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "60"));
    }

    @Test
    @DisplayName("X-Forwarded-For 로 다른 IP 로 들어오면 차단되지 않음 (키 독립)")
    void login_from_different_ip_not_blocked() throws Exception {
        // given: 첫 IP 로 5회 소진
        Map<String, Object> badCreds = Map.of(
                "email", "nobody@gout.test",
                "password", "wrong-password"
        );
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .header("X-Forwarded-For", ATTACKER_IP)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(badCreds)))
                    .andExpect(status().isUnauthorized());
        }

        // when: 동일한 remoteAddr 이지만 X-Forwarded-For 가 다른 IP
        // then: 새 버킷 → 401 (429 아님)
        mockMvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", OTHER_IP)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(badCreds)))
                .andExpect(status().isUnauthorized());
    }
}

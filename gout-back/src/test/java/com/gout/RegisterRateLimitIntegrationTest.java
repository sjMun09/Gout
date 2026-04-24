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
 * HIGH-001: 회원가입 엔드포인트 IP 기반 레이트 리밋 (10 requests / 10 min / IP).
 *
 * <p>BCrypt cost-12 해시 연산을 포함하므로 login 보다 CPU 비용이 크다.
 * 과도한 가입 요청 → CPU DoS / 이메일 열거 / 계정 폭탄 방어.
 *
 * given-when-then BDD 스타일.
 */
class RegisterRateLimitIntegrationTest extends IntegrationTestBase {

    private static final String ATTACKER_IP = "10.0.0.77";
    private static final String OTHER_IP    = "10.0.0.78";

    private Map<String, Object> uniqueRegisterBody(int index) {
        return Map.of(
                "email",    "reg-spam-" + index + "@gout.test",
                "password", "password123",
                "nickname", "스팸계정" + index
        );
    }

    @Test
    @DisplayName("회원가입 10회 이하는 정상 처리 (429 아님)")
    void register_under_limit_succeeds() throws Exception {
        // when / then: 9회 시도 — 모두 200 (버킷 1개 남음)
        for (int i = 0; i < 9; i++) {
            mockMvc.perform(post("/api/auth/register")
                            .with(req -> { req.setRemoteAddr(ATTACKER_IP); return req; })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(uniqueRegisterBody(i))))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("같은 IP에서 11번째 회원가입 시도 시 429 + Retry-After: 60")
    void register_exceeds_limit_returns429() throws Exception {
        // when: 10회 소비 → 버킷 empty
        for (int i = 0; i < 10; i++) {
            final int idx = i;
            mockMvc.perform(post("/api/auth/register")
                            .with(req -> { req.setRemoteAddr(ATTACKER_IP); return req; })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(uniqueRegisterBody(idx))))
                    .andExpect(status().isOk());
        }

        // then: 11번째는 429 + Retry-After 헤더
        mockMvc.perform(post("/api/auth/register")
                        .with(req -> { req.setRemoteAddr(ATTACKER_IP); return req; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(uniqueRegisterBody(99))))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "60"));
    }

    @Test
    @DisplayName("다른 IP 는 독립 버킷 — A IP 소진 후에도 B IP 는 정상 처리")
    void register_different_ip_has_independent_bucket() throws Exception {
        // given: ATTACKER_IP 10회 소진
        for (int i = 0; i < 10; i++) {
            final int idx = i;
            mockMvc.perform(post("/api/auth/register")
                            .with(req -> { req.setRemoteAddr(ATTACKER_IP); return req; })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(uniqueRegisterBody(idx))))
                    .andExpect(status().isOk());
        }

        // when: OTHER_IP 는 아직 버킷 가득 — 첫 시도는 200
        mockMvc.perform(post("/api/auth/register")
                        .with(req -> { req.setRemoteAddr(OTHER_IP); return req; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(uniqueRegisterBody(200))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("P1-11 회귀: X-Forwarded-For 변조로는 register bucket 을 분리할 수 없다")
    void register_xff_tamper_cannot_bypass_bucket() throws Exception {
        // given: 동일 TCP 소스에서 10회 회원가입 (XFF 는 ATTACKER_IP 로 붙이지만 무시)
        for (int i = 0; i < 10; i++) {
            final int idx = i;
            mockMvc.perform(post("/api/auth/register")
                            .header("X-Forwarded-For", ATTACKER_IP)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(uniqueRegisterBody(idx))))
                    .andExpect(status().isOk());
        }

        // when: 같은 TCP 소스인데 XFF 만 OTHER_IP 로 변조
        // then: request.getRemoteAddr() 만 보므로 동일 bucket → 429 + Retry-After
        mockMvc.perform(post("/api/auth/register")
                        .header("X-Forwarded-For", OTHER_IP)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(uniqueRegisterBody(300))))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "60"));
    }
}

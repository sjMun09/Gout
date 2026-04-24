package com.gout;

import com.gout.security.JwtTokenProvider;
import com.gout.security.RefreshTokenStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import tools.jackson.databind.JsonNode;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P1-8: 리프레시 토큰 Redis 저장 + 로테이션 검증.
 *
 * - 로그인/재발급 시 Redis 에 저장되는가
 * - 재발급 후 이전 리프레시 토큰은 거부되는가 (로테이션)
 * - logout/invalidate 후 재발급이 차단되는가
 * - 서명 변조 JWT 는 signature 검증 단계에서 거부되는가 (defense-in-depth)
 */
class RefreshTokenRotationIntegrationTest extends IntegrationTestBase {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private RefreshTokenStore refreshTokenStore;

    @Test
    @DisplayName("로그인 시 리프레시 토큰이 Redis 에 저장된다")
    void login_storesRefreshTokenInRedis() throws Exception {
        // given
        String email = "store@gout.test";
        register(email, "password123", "저장유저");

        // when
        JsonNode loginResp = loginJson(email, "password123");
        String refreshToken = loginResp.path("data").path("refreshToken").asText();
        String userId = jwtTokenProvider.getUserId(refreshToken);

        // then
        assertThat(refreshToken).isNotBlank();
        String stored = redisTemplate.opsForValue().get("refresh:" + userId);
        assertThat(stored).isEqualTo(refreshToken);
    }

    @Test
    @DisplayName("재발급 시 새 리프레시 토큰으로 로테이션되고 이전 토큰은 무효화된다")
    void refresh_rotatesRefreshToken() throws Exception {
        // given
        String email = "rotate@gout.test";
        register(email, "password123", "로테이션유저");
        JsonNode loginResp = loginJson(email, "password123");
        String oldRefresh = loginResp.path("data").path("refreshToken").asText();

        // when
        JsonNode refreshResp = callRefresh(oldRefresh);
        String newRefresh = refreshResp.path("data").path("refreshToken").asText();

        // then
        assertThat(newRefresh).isNotBlank().isNotEqualTo(oldRefresh);
        String userId = jwtTokenProvider.getUserId(newRefresh);
        String stored = redisTemplate.opsForValue().get("refresh:" + userId);
        assertThat(stored).isEqualTo(newRefresh);

        // then - 이전 토큰으로 다시 시도하면 401
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("refreshToken", oldRefresh))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("invalidate(로그아웃) 후 리프레시 토큰으로 재발급하면 401")
    void refresh_withInvalidatedToken_returns401() throws Exception {
        // given
        String email = "logout@gout.test";
        register(email, "password123", "로그아웃유저");
        JsonNode loginResp = loginJson(email, "password123");
        String refreshToken = loginResp.path("data").path("refreshToken").asText();
        String userId = jwtTokenProvider.getUserId(refreshToken);

        // when
        refreshTokenStore.invalidate(userId);

        // then
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("서명이 변조된 JWT 는 서명 검증 단계에서 거부된다")
    void refresh_withTamperedToken_returns4xx() throws Exception {
        // given
        String email = "tamper@gout.test";
        register(email, "password123", "변조유저");
        JsonNode loginResp = loginJson(email, "password123");
        String refreshToken = loginResp.path("data").path("refreshToken").asText();

        // when — 서명부(마지막 세그먼트) 를 다른 Base64URL 문자로 바꿔 시그니처 깨뜨리기
        String[] parts = refreshToken.split("\\.");
        assertThat(parts).hasSize(3);
        String tampered = parts[0] + "." + parts[1] + "." + flipLastChar(parts[2]);

        // then
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("refreshToken", tampered))))
                .andExpect(status().is4xxClientError());
    }

    // ======== helpers ========

    private JsonNode loginJson(String email, String password) throws Exception {
        String body = toJson(Map.of("email", email, "password", password));
        String json = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is2xxSuccessful())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(json);
    }

    private JsonNode callRefresh(String refreshToken) throws Exception {
        String body = toJson(Map.of("refreshToken", refreshToken));
        String json = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is2xxSuccessful())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(json);
    }

    private String flipLastChar(String s) {
        char last = s.charAt(s.length() - 1);
        // Base64URL 알파벳 내에서 다른 문자로 교체
        char replacement = last == 'A' ? 'B' : 'A';
        return s.substring(0, s.length() - 1) + replacement;
    }
}

package com.gout;

import com.gout.security.JwtTokenProvider;
import com.gout.security.JwtTokenProvider.ParsedToken;
import com.gout.security.RefreshTokenStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import tools.jackson.databind.JsonNode;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P1-8/P1-10: 리프레시 토큰 Redis 저장 + 로테이션 + 재사용 탐지 검증.
 */
class RefreshTokenRotationIntegrationTest extends IntegrationTestBase {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private RefreshTokenStore refreshTokenStore;

    @Test
    @DisplayName("로그인 시 리프레시 토큰(jti)이 Redis 에 저장된다")
    void login_storesRefreshTokenInRedis() throws Exception {
        String email = "store@gout.test";
        register(email, "Password123!", "저장유저");

        JsonNode loginResp = loginJson(email, "Password123!");
        String refreshToken = loginResp.path("data").path("refreshToken").asText();
        ParsedToken parsed = jwtTokenProvider.parseRefresh(refreshToken);

        assertThat(refreshToken).isNotBlank();
        assertThat(refreshTokenStore.isValid(parsed.getUserId(), parsed.getJti())).isTrue();
    }

    @Test
    @DisplayName("재발급 시 새 리프레시 토큰으로 로테이션되고 이전 토큰은 used 로 전환된다")
    void refresh_rotatesRefreshToken() throws Exception {
        String email = "rotate@gout.test";
        register(email, "Password123!", "로테이션유저");
        JsonNode loginResp = loginJson(email, "Password123!");
        String oldRefresh = loginResp.path("data").path("refreshToken").asText();
        ParsedToken oldParsed = jwtTokenProvider.parseRefresh(oldRefresh);

        JsonNode refreshResp = callRefresh(oldRefresh);
        String newRefresh = refreshResp.path("data").path("refreshToken").asText();
        ParsedToken newParsed = jwtTokenProvider.parseRefresh(newRefresh);

        assertThat(newRefresh).isNotBlank().isNotEqualTo(oldRefresh);
        assertThat(newParsed.getJti()).isNotEqualTo(oldParsed.getJti());
        assertThat(refreshTokenStore.isValid(newParsed.getUserId(), newParsed.getJti())).isTrue();
        assertThat(refreshTokenStore.isUsed(oldParsed.getUserId(), oldParsed.getJti())).isTrue();

        // 이전 토큰으로 재시도 → used 탐지로 401 + 전체 세션 폐기
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("refreshToken", oldRefresh))))
                .andExpect(status().isUnauthorized());

        // 전체 세션 폐기 이후에는 새 토큰도 거부되어야 한다(재사용 탐지 정책).
        assertThat(refreshTokenStore.isValid(newParsed.getUserId(), newParsed.getJti())).isFalse();
    }

    @Test
    @DisplayName("invalidate(로그아웃) 후 리프레시 토큰으로 재발급하면 401")
    void refresh_withInvalidatedToken_returns401() throws Exception {
        String email = "logout@gout.test";
        register(email, "Password123!", "로그아웃유저");
        JsonNode loginResp = loginJson(email, "Password123!");
        String refreshToken = loginResp.path("data").path("refreshToken").asText();
        ParsedToken parsed = jwtTokenProvider.parseRefresh(refreshToken);

        refreshTokenStore.invalidateAll(parsed.getUserId());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("서명이 변조된 JWT 는 서명 검증 단계에서 거부된다")
    void refresh_withTamperedToken_returns4xx() throws Exception {
        String email = "tamper@gout.test";
        register(email, "Password123!", "변조유저");
        JsonNode loginResp = loginJson(email, "Password123!");
        String refreshToken = loginResp.path("data").path("refreshToken").asText();

        String[] parts = refreshToken.split("\\.");
        assertThat(parts).hasSize(3);
        String tampered = parts[0] + "." + parts[1] + "." + flipLastChar(parts[2]);

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
        char replacement = last == 'A' ? 'B' : 'A';
        return s.substring(0, s.length() - 1) + replacement;
    }
}

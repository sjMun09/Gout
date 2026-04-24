package com.gout;

import com.gout.global.exception.ErrorCode;
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
        // #45: status 뿐 아니라 ApiResponse.message 가 ErrorCode.INVALID_TOKEN 과 일치하는지 검증.
        // 재사용 탐지 경로(AuthServiceImpl:120) 와 동일한 에러 코드를 뱉어야 한다.
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("refreshToken", oldRefresh))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(ErrorCode.INVALID_TOKEN.getMessage()));

        // 전체 세션 폐기 이후에는 새 토큰도 거부되어야 한다(재사용 탐지 정책).
        assertThat(refreshTokenStore.isValid(newParsed.getUserId(), newParsed.getJti())).isFalse();

        // 새 토큰으로도 401 + 동일 메시지
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("refreshToken", newRefresh))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(ErrorCode.INVALID_TOKEN.getMessage()));
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

        // #45: status + success 외에 message 도 ErrorCode.INVALID_TOKEN 과 일치해야 한다.
        // 로그아웃 경로(AuthServiceImpl:125) 도 동일 에러코드를 유지해야 재사용 탐지와 회귀 없이 구분 가능.
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(ErrorCode.INVALID_TOKEN.getMessage()));
    }

    @Test
    @DisplayName("P2-46: invalidateAll 은 valid / used 두 네임스페이스를 각각 SCAN 해서 모두 삭제한다")
    void invalidateAll_removesBothValidAndUsedKeys() throws Exception {
        String email = "invall@gout.test";
        register(email, "Password123!", "무효화유저");

        // given: 로그인 2회 (multi-device 가정) → 두 jti 가 valid 네임스페이스에 존재
        JsonNode first = loginJson(email, "Password123!");
        ParsedToken firstParsed = jwtTokenProvider.parseRefresh(first.path("data").path("refreshToken").asText());
        JsonNode second = loginJson(email, "Password123!");
        ParsedToken secondParsed = jwtTokenProvider.parseRefresh(second.path("data").path("refreshToken").asText());
        String userId = firstParsed.getUserId();
        assertThat(userId).isEqualTo(secondParsed.getUserId());

        // 첫 토큰을 명시적으로 used 네임스페이스로 이동 — valid / used 둘 다 존재하는 상태
        refreshTokenStore.tryMarkUsed(userId, firstParsed.getJti(), 60L);
        assertThat(refreshTokenStore.isUsed(userId, firstParsed.getJti())).isTrue();
        assertThat(refreshTokenStore.isValid(userId, secondParsed.getJti())).isTrue();

        // when: invalidateAll
        refreshTokenStore.invalidateAll(userId);

        // then: valid / used 양쪽 모두 제거
        assertThat(refreshTokenStore.isUsed(userId, firstParsed.getJti())).isFalse();
        assertThat(refreshTokenStore.isValid(userId, secondParsed.getJti())).isFalse();
    }

    @Test
    @DisplayName("P2-43: 동일 jti 로 tryMarkUsed 를 두 번 호출하면 두 번째는 false — SETNX 아토믹")
    void tryMarkUsed_isAtomic_onlyOneCallerWins() throws Exception {
        String email = "race@gout.test";
        register(email, "Password123!", "레이스유저");
        JsonNode loginResp = loginJson(email, "Password123!");
        String refreshToken = loginResp.path("data").path("refreshToken").asText();
        ParsedToken parsed = jwtTokenProvider.parseRefresh(refreshToken);

        // given: 로그인 직후 validKey 존재, usedKey 없음
        assertThat(refreshTokenStore.isValid(parsed.getUserId(), parsed.getJti())).isTrue();
        assertThat(refreshTokenStore.isUsed(parsed.getUserId(), parsed.getJti())).isFalse();

        // when / then: 첫 호출만 true, 두 번째는 false (usedKey 가 이미 SET NX 로 존재)
        boolean first = refreshTokenStore.tryMarkUsed(parsed.getUserId(), parsed.getJti(), 60L);
        boolean second = refreshTokenStore.tryMarkUsed(parsed.getUserId(), parsed.getJti(), 60L);
        assertThat(first).isTrue();
        assertThat(second).isFalse();

        // 그리고 validKey 는 첫 성공 호출에서 제거되었어야 한다
        assertThat(refreshTokenStore.isValid(parsed.getUserId(), parsed.getJti())).isFalse();
        assertThat(refreshTokenStore.isUsed(parsed.getUserId(), parsed.getJti())).isTrue();
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

        // #45: JwtException 경로(AuthServiceImpl:110) 도 INVALID_TOKEN 메시지로 통일되어야 한다.
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("refreshToken", tampered))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(ErrorCode.INVALID_TOKEN.getMessage()));
    }

    @Test
    @DisplayName("#45: 존재하지 않는 jti(로그인 이력 없음) 로 refresh 요청 — 401 + INVALID_TOKEN")
    void refresh_withUnknownJti_returns401_withInvalidTokenMessage() throws Exception {
        String email = "unknown@gout.test";
        register(email, "Password123!", "유령유저");
        JsonNode loginResp = loginJson(email, "Password123!");
        String refreshToken = loginResp.path("data").path("refreshToken").asText();
        ParsedToken parsed = jwtTokenProvider.parseRefresh(refreshToken);

        // valid 네임스페이스에서만 선제 제거 → used 도 아니고 valid 도 아닌 "unknown" 상태 유도
        refreshTokenStore.invalidateAll(parsed.getUserId());

        // AuthServiceImpl:125 (isValid false 경로) 진입을 기대.
        // 과거 회귀에서는 이 경로가 EXPIRED_TOKEN 이나 500 으로 흐를 가능성이 있어 명시 검증.
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(ErrorCode.INVALID_TOKEN.getMessage()));
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

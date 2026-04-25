package com.gout;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthFlowIntegrationTest extends IntegrationTestBase {

    @Test
    @DisplayName("회원가입 성공 → accessToken 발급")
    void register_success() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of(
                                "email", "new1@gout.test",
                                "password", "password123",
                                "nickname", "신규유저"
                        ))))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());
    }

    @Test
    @DisplayName("중복 이메일 회원가입 실패")
    void register_duplicate_email_fails() throws Exception {
        register("dup@gout.test", "password123", "중복유저");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of(
                                "email", "dup@gout.test",
                                "password", "password123",
                                "nickname", "중복유저2"
                        ))))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("로그인 성공 → accessToken 추출 가능")
    void login_success() throws Exception {
        register("login@gout.test", "password123", "로그인유저");

        String token = login("login@gout.test", "password123");

        assertNotNull(token);
    }

    @Test
    @DisplayName("토큰 없이 보호 엔드포인트 호출 시 401 + 표준 ErrorResponse body")
    void protected_endpoint_without_token() throws Exception {
        mockMvc.perform(get("/api/health/uric-acid-logs"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/api/health/uric-acid-logs"));
    }

    @Test
    @DisplayName("Bearer 토큰으로 보호 엔드포인트 호출 성공")
    void protected_endpoint_with_token() throws Exception {
        String token = registerAndLogin("protected@gout.test", "password123", "보호유저");

        mockMvc.perform(get("/api/health/uric-acid-logs")
                        .header(HttpHeaders.AUTHORIZATION, authHeader(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}

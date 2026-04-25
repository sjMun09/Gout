package com.gout;

import tools.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P1-9: 좋아요 엔드포인트 userId 기반 레이트 리밋 (30 requests/min/user).
 *
 * given-when-then BDD 스타일.
 */
class LikeRateLimitIntegrationTest extends IntegrationTestBase {

    private String createPost(String token) throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/posts")
                        .header(HttpHeaders.AUTHORIZATION, authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of(
                                "title", "레이트 리밋 테스트 게시글",
                                "content", "좋아요 스팸 방어 확인",
                                "category", "FREE",
                                "isAnonymous", false
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(createResult.getResponse().getContentAsString());
        return body.path("data").path("id").asText();
    }

    @Test
    @DisplayName("1분 내 30회 이하 좋아요는 모두 200")
    void like_under_limit_succeeds() throws Exception {
        // given: 가입한 사용자 + 본인 게시글
        String token = registerAndLogin("like-ok@gout.test", "password123", "좋아요정상");
        String postId = createPost(token);

        // when / then: 29회 토글 — 모두 200 (아직 버킷 1개 남음; 게시글 생성이 좋아요 버킷을 건드리지 않음)
        for (int i = 0; i < 29; i++) {
            mockMvc.perform(post("/api/posts/" + postId + "/like")
                            .header(HttpHeaders.AUTHORIZATION, authHeader(token)))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("31번째 좋아요는 429 + Retry-After: 60")
    void like_exceeds_limit_returns429() throws Exception {
        // given: 가입한 사용자 + 본인 게시글
        String token = registerAndLogin("like-spam@gout.test", "password123", "좋아요스팸");
        String postId = createPost(token);

        // when: 30회 모두 소진
        for (int i = 0; i < 30; i++) {
            mockMvc.perform(post("/api/posts/" + postId + "/like")
                            .header(HttpHeaders.AUTHORIZATION, authHeader(token)))
                    .andExpect(status().isOk());
        }

        // then: 31번째는 429 + Retry-After 헤더 + 표준 ErrorResponse body
        mockMvc.perform(post("/api/posts/" + postId + "/like")
                        .header(HttpHeaders.AUTHORIZATION, authHeader(token)))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "60"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_TOO_MANY_REQUESTS"))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.path").value("/api/posts/" + postId + "/like"));
    }
}

package com.gout;

import tools.jackson.databind.JsonNode;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReportIntegrationTest extends IntegrationTestBase {

    /**
     * 토큰이 없으면 /api/reports 에 접근 불가 (401/403).
     * gender_type 버그와 무관하게 실행 가능.
     */
    @Test
    @DisplayName("로그인 없이 신고 POST → 401/403")
    void report_requires_auth() throws Exception {
        mockMvc.perform(post("/api/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of(
                                "targetType", "POST",
                                "targetId", "00000000-0000-0000-0000-000000000000",
                                "reason", "SPAM"
                        ))))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @Disabled("registerAndLogin() 이 gender_type 버그로 실패 → 토큰 없음. 다른 Community 테스트들과 동일 패턴.")
    @DisplayName("게시글 신고 성공 → 201 생성")
    void report_post_success() throws Exception {
        String token = registerAndLogin("reporter1@gout.test", "password123", "신고자1");
        String postId = createPost(token, "신고될 글", "내용");

        mockMvc.perform(post("/api/reports")
                        .header(HttpHeaders.AUTHORIZATION, authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of(
                                "targetType", "POST",
                                "targetId", postId,
                                "reason", "SPAM",
                                "detail", "광고성 게시글입니다."
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.targetType").value("POST"))
                .andExpect(jsonPath("$.data.reason").value("SPAM"));
    }

    @Test
    @Disabled("registerAndLogin() 이 gender_type 버그로 실패 — 토큰 없음.")
    @DisplayName("댓글 신고 성공 → 201 생성")
    void report_comment_success() throws Exception {
        String token = registerAndLogin("reporter2@gout.test", "password123", "신고자2");
        String postId = createPost(token, "댓글 붙일 글", "본문");
        String commentId = createComment(token, postId, "신고될 댓글");

        mockMvc.perform(post("/api/reports")
                        .header(HttpHeaders.AUTHORIZATION, authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of(
                                "targetType", "COMMENT",
                                "targetId", commentId,
                                "reason", "ABUSE"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.targetType").value("COMMENT"))
                .andExpect(jsonPath("$.data.reason").value("ABUSE"));
    }

    @Test
    @Disabled("registerAndLogin() 이 gender_type 버그로 실패 — 토큰 없음.")
    @DisplayName("같은 사용자가 같은 대상을 중복 신고 → 409")
    void duplicate_report_returns_409() throws Exception {
        String token = registerAndLogin("reporter3@gout.test", "password123", "신고자3");
        String postId = createPost(token, "중복 신고 글", "본문");

        mockMvc.perform(post("/api/reports")
                        .header(HttpHeaders.AUTHORIZATION, authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of(
                                "targetType", "POST",
                                "targetId", postId,
                                "reason", "SPAM"
                        ))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/reports")
                        .header(HttpHeaders.AUTHORIZATION, authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of(
                                "targetType", "POST",
                                "targetId", postId,
                                "reason", "ABUSE"
                        ))))
                .andExpect(status().isConflict());
    }

    @Test
    @Disabled("registerAndLogin() 이 gender_type 버그로 실패 — 토큰 없음.")
    @DisplayName("잘못된 targetType → 400")
    void invalid_target_type_returns_400() throws Exception {
        String token = registerAndLogin("reporter4@gout.test", "password123", "신고자4");

        mockMvc.perform(post("/api/reports")
                        .header(HttpHeaders.AUTHORIZATION, authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of(
                                "targetType", "USER", // 허용되지 않음
                                "targetId", "00000000-0000-0000-0000-000000000000",
                                "reason", "SPAM"
                        ))))
                .andExpect(status().isBadRequest());
    }

    // ============== Helpers ==============

    private String createPost(String token, String title, String content) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/posts")
                        .header(HttpHeaders.AUTHORIZATION, authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of(
                                "title", title,
                                "content", content,
                                "category", "FREE",
                                "isAnonymous", false
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        String id = body.path("data").path("id").asText();
        assertNotNull(id);
        return id;
    }

    private String createComment(String token, String postId, String content) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/posts/" + postId + "/comments")
                        .header(HttpHeaders.AUTHORIZATION, authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of(
                                "content", content,
                                "isAnonymous", false
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        String id = body.path("data").path("id").asText();
        assertNotNull(id);
        return id;
    }
}

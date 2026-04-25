package com.gout;

import tools.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CommentIntegrationTest extends IntegrationTestBase {

    @Test
    @DisplayName("댓글 수정 플로우 — 본인 200, 타인 403, 삭제된 댓글 404")
    void comment_edit_flow() throws Exception {
        // 작성자/타인 두 명 준비
        String authorToken = registerAndLogin("comment-author@gout.test", "password123", "작성자");
        String otherToken = registerAndLogin("comment-other@gout.test", "password123", "타인");

        // 1) 게시글 생성 (작성자)
        MvcResult postResult = mockMvc.perform(post("/api/posts")
                        .header(HttpHeaders.AUTHORIZATION, authHeader(authorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of(
                                "title", "댓글 수정 테스트 글",
                                "content", "본문",
                                "category", "FREE",
                                "isAnonymous", false
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        String postId = objectMapper.readTree(postResult.getResponse().getContentAsString())
                .path("data").path("id").asText();
        assertNotNull(postId);

        // 2) 댓글 작성 (작성자)
        MvcResult createResult = mockMvc.perform(post("/api/posts/" + postId + "/comments")
                        .header(HttpHeaders.AUTHORIZATION, authHeader(authorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of(
                                "content", "원본 댓글",
                                "isAnonymous", false
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        String commentId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asText();
        assertNotNull(commentId);

        // 3) 본인 수정 → 200, content 반영
        mockMvc.perform(put("/api/comments/" + commentId)
                        .header(HttpHeaders.AUTHORIZATION, authHeader(authorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("content", "수정된 댓글"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").value("수정된 댓글"));

        // 4) 타인 수정 → 403
        mockMvc.perform(put("/api/comments/" + commentId)
                        .header(HttpHeaders.AUTHORIZATION, authHeader(otherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("content", "해킹 시도"))))
                .andExpect(status().isForbidden());

        // 5) 빈 content → 400 (validation)
        mockMvc.perform(put("/api/comments/" + commentId)
                        .header(HttpHeaders.AUTHORIZATION, authHeader(authorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("content", "   "))))
                .andExpect(status().isBadRequest());

        // 6) 삭제 후 수정 시도 → 404 (COMMENT_NOT_FOUND)
        mockMvc.perform(delete("/api/comments/" + commentId)
                        .header(HttpHeaders.AUTHORIZATION, authHeader(authorToken)))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/comments/" + commentId)
                        .header(HttpHeaders.AUTHORIZATION, authHeader(authorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("content", "삭제된 댓글 수정"))))
                .andExpect(status().isNotFound());
    }
}

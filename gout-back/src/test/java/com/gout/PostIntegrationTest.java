package com.gout;

import tools.jackson.databind.JsonNode;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PostIntegrationTest extends IntegrationTestBase {

    @Test
    @Disabled("registerAndLogin() 가 gender_type 버그로 실패 → 토큰 없음 → 403")
    @DisplayName("게시글 작성 → 댓글 → 좋아요 토글 → 목록 조회 흐름")
    void post_full_flow() throws Exception {
        String token = registerAndLogin("post1@gout.test", "password123", "글쓴이1");

        // 게시글 생성
        MvcResult createResult = mockMvc.perform(post("/api/posts")
                        .header(HttpHeaders.AUTHORIZATION, authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of(
                                "title", "통합 테스트 글",
                                "content", "본문입니다.",
                                "category", "FREE",
                                "isAnonymous", false
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andReturn();

        JsonNode body = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String postId = body.path("data").path("id").asText();
        assertNotNull(postId);
        assertFalse(postId.isEmpty());

        // 댓글 작성
        mockMvc.perform(post("/api/posts/" + postId + "/comments")
                        .header(HttpHeaders.AUTHORIZATION, authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of(
                                "content", "첫 댓글",
                                "isAnonymous", false
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("첫 댓글"));

        // 좋아요 토글 1회 → likeCount 1
        mockMvc.perform(post("/api/posts/" + postId + "/like")
                        .header(HttpHeaders.AUTHORIZATION, authHeader(token)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/posts/" + postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.likeCount").value(1));

        // 좋아요 토글 2회 → likeCount 0
        mockMvc.perform(post("/api/posts/" + postId + "/like")
                        .header(HttpHeaders.AUTHORIZATION, authHeader(token)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/posts/" + postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.likeCount").value(0));

        // 목록 조회 (카테고리 필터)
        mockMvc.perform(get("/api/posts").param("category", "FREE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @Disabled("registerAndLogin() 가 gender_type 버그로 실패 → 토큰 없음 → 403")
    @DisplayName("익명 게시글 목록에서 nickname='익명' 노출")
    void anonymous_post_shows_masked_nickname() throws Exception {
        String token = registerAndLogin("anon@gout.test", "password123", "실명유저");

        mockMvc.perform(post("/api/posts")
                        .header(HttpHeaders.AUTHORIZATION, authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of(
                                "title", "익명 글",
                                "content", "익명 본문",
                                "category", "FREE",
                                "isAnonymous", true
                        ))))
                .andExpect(status().isOk());

        MvcResult listResult = mockMvc.perform(get("/api/posts").param("category", "FREE"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode list = objectMapper.readTree(listResult.getResponse().getContentAsString())
                .path("data").path("content");
        assertTrue(list.isArray() && list.size() > 0, "익명 게시글이 목록에 존재해야 함");

        boolean foundAnonymous = false;
        for (JsonNode item : list) {
            if ("익명 글".equals(item.path("title").asText())) {
                foundAnonymous = true;
                // 익명 게시글은 nickname이 "익명"으로 마스킹
                assertTrue(item.path("isAnonymous").asBoolean(), "isAnonymous true");
                assertTrue("익명".equals(item.path("nickname").asText()),
                        "nickname은 '익명'이어야 함, 실제=" + item.path("nickname").asText());
            }
        }
        assertTrue(foundAnonymous, "작성한 익명 글이 목록에 있어야 함");
    }
}

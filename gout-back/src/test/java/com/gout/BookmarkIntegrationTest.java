package com.gout;

import tools.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BookmarkIntegrationTest extends IntegrationTestBase {

    @Test
    @DisplayName("토큰 없이 북마크 토글 호출 시 401/403")
    void toggle_without_token_returns_unauthorized() throws Exception {
        mockMvc.perform(post("/api/posts/any-post-id/bookmark"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("토큰 없이 내 북마크 목록 호출 시 401/403")
    void list_without_token_returns_unauthorized() throws Exception {
        mockMvc.perform(get("/api/me/bookmarks"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("토큰 없이 북마크 상태 조회 시 401 (permitAll 이지만 컨트롤러에서 거부)")
    void status_without_token_returns_unauthorized() throws Exception {
        mockMvc.perform(get("/api/posts/any-post-id/bookmark-status"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("북마크 토글 on → off 흐름, 카운트/상태 동기화")
    void bookmark_toggle_flow() throws Exception {
        String token = registerAndLogin("bm1@gout.test", "password123", "북마커1");

        MvcResult createResult = mockMvc.perform(post("/api/posts")
                        .header(HttpHeaders.AUTHORIZATION, authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of(
                                "title", "북마크 테스트 글",
                                "content", "본문",
                                "category", "FREE",
                                "isAnonymous", false
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String postId = body.path("data").path("id").asText();
        assertNotNull(postId);

        // 최초 상태: bookmarked=false
        mockMvc.perform(get("/api/posts/" + postId + "/bookmark-status")
                        .header(HttpHeaders.AUTHORIZATION, authHeader(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bookmarked").value(false));

        // 토글 1회 → bookmarked=true
        mockMvc.perform(post("/api/posts/" + postId + "/bookmark")
                        .header(HttpHeaders.AUTHORIZATION, authHeader(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bookmarked").value(true));

        // 상세 조회: bookmarkCount=1, bookmarked=true
        mockMvc.perform(get("/api/posts/" + postId)
                        .header(HttpHeaders.AUTHORIZATION, authHeader(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bookmarkCount").value(1))
                .andExpect(jsonPath("$.data.bookmarked").value(true));

        // 내 북마크 목록에 포함
        mockMvc.perform(get("/api/me/bookmarks")
                        .header(HttpHeaders.AUTHORIZATION, authHeader(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(postId));

        // 토글 2회 → bookmarked=false
        mockMvc.perform(post("/api/posts/" + postId + "/bookmark")
                        .header(HttpHeaders.AUTHORIZATION, authHeader(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bookmarked").value(false));

        // 상세 조회: bookmarkCount=0
        mockMvc.perform(get("/api/posts/" + postId)
                        .header(HttpHeaders.AUTHORIZATION, authHeader(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bookmarkCount").value(0))
                .andExpect(jsonPath("$.data.bookmarked").value(false));
    }
}

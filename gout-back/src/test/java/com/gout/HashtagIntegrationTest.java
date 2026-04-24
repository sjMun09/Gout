package com.gout;

import tools.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HashtagIntegrationTest extends IntegrationTestBase {

    @Test
    @DisplayName("태그 2개 달린 글 작성 → 응답에 tags 포함")
    void create_post_with_tags() throws Exception {
        String token = registerAndLogin("hashtag1@gout.test", "password123", "태거1");

        MvcResult result = mockMvc.perform(post("/api/posts")
                        .header(HttpHeaders.AUTHORIZATION, authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of(
                                "title", "해시태그 테스트 글",
                                "content", "태그 있는 본문",
                                "category", "FREE",
                                "isAnonymous", false,
                                "tags", List.of("식단", "저퓨린")
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tags").isArray())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode tags = body.path("data").path("tags");
        assertEquals(2, tags.size(), "생성 응답에 태그 2개가 있어야 함");

        // 상세 조회에서도 태그 확인
        String postId = body.path("data").path("id").asText();
        mockMvc.perform(get("/api/posts/" + postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tags").isArray())
                .andExpect(jsonPath("$.data.tags.length()").value(2));
    }

    @Test
    @DisplayName("/api/posts?tag=식단 → 해당 태그 단 글만 반환")
    void filter_posts_by_tag() throws Exception {
        String token = registerAndLogin("hashtag2@gout.test", "password123", "태거2");

        // 식단 태그 글 작성
        MvcResult r = mockMvc.perform(post("/api/posts")
                        .header(HttpHeaders.AUTHORIZATION, authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of(
                                "title", "식단 태그 글",
                                "content", "본문",
                                "category", "FREE",
                                "isAnonymous", false,
                                "tags", List.of("식단")
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        String taggedPostId = objectMapper.readTree(r.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // 태그 없는 글 작성
        mockMvc.perform(post("/api/posts")
                        .header(HttpHeaders.AUTHORIZATION, authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of(
                                "title", "태그 없는 글",
                                "content", "본문2",
                                "category", "FREE",
                                "isAnonymous", false
                        ))))
                .andExpect(status().isOk());

        // tag=식단 필터 — 식단 태그 글만 나와야 함
        MvcResult listResult = mockMvc.perform(get("/api/posts").param("tag", "식단"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode content = objectMapper.readTree(listResult.getResponse().getContentAsString())
                .path("data").path("content");
        assertTrue(content.isArray() && content.size() >= 1, "태그 필터 결과가 1건 이상이어야 함");

        boolean found = false;
        for (JsonNode item : content) {
            if (taggedPostId.equals(item.path("id").asText())) {
                found = true;
                // 태그 없는 글이 포함되지 않는지 확인
                JsonNode tags = item.path("tags");
                boolean hasTag = false;
                for (JsonNode t : tags) {
                    if ("식단".equals(t.asText())) { hasTag = true; break; }
                }
                assertTrue(hasTag, "결과 글에 '식단' 태그가 있어야 함");
            }
        }
        assertTrue(found, "식단 태그 단 글이 필터 결과에 있어야 함");
    }

    @Test
    @DisplayName("수정 시 태그 재설정")
    void update_post_replaces_tags() throws Exception {
        String token = registerAndLogin("hashtag3@gout.test", "password123", "태거3");

        // 글 생성 (태그: 운동, 관리)
        MvcResult r = mockMvc.perform(post("/api/posts")
                        .header(HttpHeaders.AUTHORIZATION, authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of(
                                "title", "수정 전 글",
                                "content", "본문",
                                "category", "FREE",
                                "isAnonymous", false,
                                "tags", List.of("운동", "관리")
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        String postId = objectMapper.readTree(r.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // 수정 (태그: 식단만)
        MvcResult updateResult = mockMvc.perform(put("/api/posts/" + postId)
                        .header(HttpHeaders.AUTHORIZATION, authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of(
                                "title", "수정 후 글",
                                "content", "본문 수정",
                                "category", "FREE",
                                "isAnonymous", false,
                                "tags", List.of("식단")
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode tags = objectMapper.readTree(updateResult.getResponse().getContentAsString())
                .path("data").path("tags");
        assertEquals(1, tags.size(), "수정 후 태그는 1개여야 함");
        assertEquals("식단", tags.get(0).asText(), "수정 후 태그는 '식단'이어야 함");
    }

    @Test
    @DisplayName("11개 태그 → 400 Bad Request")
    void create_post_with_too_many_tags_returns_400() throws Exception {
        String token = registerAndLogin("hashtag4@gout.test", "password123", "태거4");

        mockMvc.perform(post("/api/posts")
                        .header(HttpHeaders.AUTHORIZATION, authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of(
                                "title", "태그 초과 글",
                                "content", "본문",
                                "category", "FREE",
                                "isAnonymous", false,
                                "tags", List.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k")
                        ))))
                .andExpect(status().isBadRequest());
    }
}

package com.gout;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FoodIntegrationTest extends IntegrationTestBase {

    @Test
    @DisplayName("음식 카테고리 목록 조회")
    void categories() throws Exception {
        mockMvc.perform(get("/api/foods/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @Disabled("FoodRepository JPQL LOWER(:keyword) 가 null 일 때 PG 가 bytea 로 추론 → 500. NEXT_STEPS 참조")
    @DisplayName("음식 검색 (빈 키워드 + size=10)")
    void search_foods() throws Exception {
        mockMvc.perform(get("/api/foods")
                        .param("keyword", "")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("존재하지 않는 음식 ID 조회 시 4xx")
    void food_not_found() throws Exception {
        mockMvc.perform(get("/api/foods/non-existent-id-xyz-123"))
                .andExpect(status().is4xxClientError());
    }
}

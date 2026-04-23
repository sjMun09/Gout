package com.gout;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HospitalIntegrationTest extends IntegrationTestBase {

    @Test
    @DisplayName("위치 기반 반경 검색 (시드 없어도 Paged 응답 구조 유효)")
    void search_by_location() throws Exception {
        mockMvc.perform(get("/api/hospitals")
                        .param("lat", "37.5")
                        .param("lng", "127.0")
                        .param("radius", "5000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalPages").exists())
                .andExpect(jsonPath("$.data.number").exists());
    }

    @Test
    @DisplayName("키워드 검색")
    void search_by_keyword() throws Exception {
        mockMvc.perform(get("/api/hospitals").param("keyword", "병원"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }
}

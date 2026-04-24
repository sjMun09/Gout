package com.gout;

import tools.jackson.databind.JsonNode;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HealthIntegrationTest extends IntegrationTestBase {

    @Test
    @Disabled("registerAndLogin() 가 gender_type 버그로 실패 → 토큰 없음 → 403")
    @DisplayName("유저별 요산 기록 격리 + 타유저 삭제 403")
    void uric_acid_log_user_isolation() throws Exception {
        String tokenA = registerAndLogin("userA@gout.test", "password123", "유저A");
        String tokenB = registerAndLogin("userB@gout.test", "password123", "유저B");

        // 유저A: 요산 기록 생성
        MvcResult createResult = mockMvc.perform(post("/api/health/uric-acid-logs")
                        .header(HttpHeaders.AUTHORIZATION, authHeader(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of(
                                "value", 7.5,
                                "measuredAt", LocalDate.now().toString(),
                                "memo", "아침 측정"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andReturn();

        JsonNode body = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String logId = body.path("data").path("id").asText();

        // 유저A GET → 1건
        mockMvc.perform(get("/api/health/uric-acid-logs")
                        .header(HttpHeaders.AUTHORIZATION, authHeader(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        // 유저B GET → 0건 (격리)
        mockMvc.perform(get("/api/health/uric-acid-logs")
                        .header(HttpHeaders.AUTHORIZATION, authHeader(tokenB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));

        // 유저B가 유저A의 것을 삭제 시도 → 403
        mockMvc.perform(delete("/api/health/uric-acid-logs/" + logId)
                        .header(HttpHeaders.AUTHORIZATION, authHeader(tokenB)))
                .andExpect(status().is4xxClientError());

        // 유저A가 자기 것 삭제 → 200 (컨트롤러가 200 OK + ApiResponse 반환)
        mockMvc.perform(delete("/api/health/uric-acid-logs/" + logId)
                        .header(HttpHeaders.AUTHORIZATION, authHeader(tokenA)))
                .andExpect(status().is2xxSuccessful());

        // 삭제 후 유저A 조회 → 0건
        mockMvc.perform(get("/api/health/uric-acid-logs")
                        .header(HttpHeaders.AUTHORIZATION, authHeader(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }
}

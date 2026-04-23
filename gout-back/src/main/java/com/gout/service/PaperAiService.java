package com.gout.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 논문 abstract_en 을 Claude 로 한국어 요약.
 * - abstract_ko: 한국어 3-5문장 요약
 * - ai_summary_ko: 일반인용 쉬운 한국어 요약 2-3문장
 *
 * API 키가 비어있으면 조용히 skip.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaperAiService {

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String MODEL = "claude-sonnet-4-6";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.anthropic.api-key:}")
    private String apiKey;

    public static class SummaryResult {
        public final String abstractKo;
        public final String aiSummaryKo;

        public SummaryResult(String abstractKo, String aiSummaryKo) {
            this.abstractKo = abstractKo;
            this.aiSummaryKo = aiSummaryKo;
        }
    }

    /**
     * abstract_en 기반 한국어 요약 2종 생성. 실패/키 없음이면 null.
     */
    public SummaryResult summarize(String title, String abstractEn) {
        if (apiKey == null || apiKey.isBlank()) {
            log.debug("Anthropic API key not configured - skipping AI summary");
            return null;
        }
        if (abstractEn == null || abstractEn.isBlank()) {
            return null;
        }
        String prompt = buildPrompt(title, abstractEn);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("x-api-key", apiKey);
            headers.set("anthropic-version", ANTHROPIC_VERSION);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("model", MODEL);
            body.put("max_tokens", 1024);
            body.put("messages", List.of(Map.of(
                    "role", "user",
                    "content", prompt
            )));

            HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, headers);
            ResponseEntity<String> resp = restTemplate.postForEntity(API_URL, req, String.class);
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                log.warn("Anthropic API non-2xx: {}", resp.getStatusCode());
                return null;
            }
            JsonNode root = objectMapper.readTree(resp.getBody());
            JsonNode content = root.path("content");
            if (!content.isArray() || content.isEmpty()) {
                return null;
            }
            String text = content.get(0).path("text").asText("");
            return parseSummary(text);
        } catch (Exception e) {
            log.warn("Anthropic summarize failed: {}", e.getMessage());
            return null;
        }
    }

    private String buildPrompt(String title, String abstractEn) {
        return """
                다음은 통풍(gout) 관련 의학 논문입니다. 한국어로 두 종류의 요약을 작성하세요.

                제목: %s

                초록:
                %s

                출력 형식을 반드시 지켜주세요. 다른 말 없이 아래 형식 그대로만 응답하세요.

                [ABSTRACT_KO]
                (연구자/의료진이 이해하기 쉬운 한국어 요약. 3-5문장. 연구 목적, 방법, 주요 결과를 포함.)

                [SUMMARY_KO]
                (일반인이 이해하기 쉬운 한국어 요약. 2-3문장. 전문 용어는 풀어서 설명.)
                """.formatted(title == null ? "" : title, abstractEn);
    }

    private SummaryResult parseSummary(String text) {
        if (text == null) return null;
        String abstractKo = extractSection(text, "[ABSTRACT_KO]", "[SUMMARY_KO]");
        String summaryKo = extractSection(text, "[SUMMARY_KO]", null);
        if ((abstractKo == null || abstractKo.isBlank()) && (summaryKo == null || summaryKo.isBlank())) {
            return null;
        }
        return new SummaryResult(
                abstractKo == null ? null : abstractKo.trim(),
                summaryKo == null ? null : summaryKo.trim()
        );
    }

    private String extractSection(String text, String startMarker, String endMarker) {
        int startIdx = text.indexOf(startMarker);
        if (startIdx < 0) return null;
        startIdx += startMarker.length();
        int endIdx = endMarker == null ? text.length() : text.indexOf(endMarker, startIdx);
        if (endIdx < 0) endIdx = text.length();
        return text.substring(startIdx, endIdx).trim();
    }
}

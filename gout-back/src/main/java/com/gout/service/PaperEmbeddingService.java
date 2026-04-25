package com.gout.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gout.config.properties.OpenAiProperties;
import com.gout.dao.PaperRepository;
import com.gout.entity.Paper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

/**
 * OpenAI text-embedding-3-small(1536d)로 논문 임베딩 생성.
 * papers.embedding 은 JPA entity에 없으므로 JdbcTemplate native UPDATE로만 갱신.
 *
 * API 키가 비어있으면 조용히 skip.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaperEmbeddingService {

    private static final String API_URL = "https://api.openai.com/v1/embeddings";
    private static final String MODEL = "text-embedding-3-small";

    private final RestTemplate restTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final PaperRepository paperRepository;
    private final OpenAiProperties openAiProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // TODO: API 키 발급 필요 — OPENAI_API_KEY 환경변수 설정 시에만 임베딩 생성.
    //   미설정 시 embedPaper() 가 false 반환 → pgvector 기반 유사 논문 추천이 동작하지 않음.

    /**
     * 특정 논문 임베딩 재생성 후 DB 업데이트. 성공 시 true.
     */
    public boolean embedPaper(String paperId) {
        String apiKey = openAiProperties.apiKey();
        if (apiKey == null || apiKey.isBlank()) {
            log.debug("OpenAI API key not configured - skipping embedding");
            return false;
        }
        Paper paper = paperRepository.findById(paperId).orElse(null);
        if (paper == null) {
            log.warn("Paper not found: {}", paperId);
            return false;
        }
        String input = buildInput(paper);
        if (input.isBlank()) return false;

        float[] embedding = createEmbedding(input);
        if (embedding == null) return false;

        String vectorLiteral = toVectorLiteral(embedding);
        jdbcTemplate.update(
                "UPDATE papers SET embedding = ?::vector, updated_at = NOW() WHERE id = ?",
                vectorLiteral,
                paperId
        );
        return true;
    }

    /**
     * 논문 객체로부터 임베딩 생성 후 DB에 저장 (크롤러 경로에서 사용).
     */
    public boolean embedPaper(Paper paper) {
        if (paper == null || paper.getId() == null) return false;
        return embedPaper(paper.getId());
    }

    private String buildInput(Paper paper) {
        StringBuilder sb = new StringBuilder();
        if (paper.getTitle() != null) sb.append(paper.getTitle()).append("\n\n");
        if (paper.getAbstractEn() != null) sb.append(paper.getAbstractEn());
        return sb.toString().trim();
    }

    private float[] createEmbedding(String input) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(openAiProperties.apiKey());
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("model", MODEL);
            body.put("input", input);

            HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, headers);
            ResponseEntity<String> resp = restTemplate.postForEntity(API_URL, req, String.class);
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                log.warn("OpenAI embeddings non-2xx: {}", resp.getStatusCode());
                return null;
            }
            JsonNode root = objectMapper.readTree(resp.getBody());
            JsonNode data = root.path("data");
            if (!data.isArray() || data.isEmpty()) return null;
            JsonNode vec = data.get(0).path("embedding");
            if (!vec.isArray() || vec.isEmpty()) return null;
            float[] result = new float[vec.size()];
            for (int i = 0; i < vec.size(); i++) {
                result[i] = (float) vec.get(i).asDouble();
            }
            return result;
        } catch (Exception e) {
            log.warn("OpenAI embedding call failed: {}", e.getMessage());
            return null;
        }
    }

    private String toVectorLiteral(float[] embedding) {
        StringJoiner sj = new StringJoiner(",", "[", "]");
        for (float v : embedding) {
            sj.add(Float.toString(v));
        }
        return sj.toString();
    }
}

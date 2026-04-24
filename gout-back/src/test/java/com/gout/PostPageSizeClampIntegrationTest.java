package com.gout;

import com.gout.dao.PostRepository;
import com.gout.entity.Post;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GET /api/posts size 파라미터 clamp 통합 테스트.
 *
 * <p>Controller 계층에서 size 를 1~100 으로 clamp 하는 방어 로직을 검증한다:
 * <ul>
 *   <li>size &lt;= 0 → 기본값 20 적용</li>
 *   <li>size &gt; 100 → 100 으로 절삭</li>
 *   <li>정상 범위(1~100) → 그대로 통과</li>
 * </ul>
 *
 * <p>DB 조작은 JdbcTemplate 으로 직접 seed (User 엔티티 gender enum 버그 회피).
 */
class PostPageSizeClampIntegrationTest extends IntegrationTestBase {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String SEED_USER_ID = "page-size-clamp-seed-user";

    @BeforeEach
    void seedPosts() {
        jdbcTemplate.update("DELETE FROM posts");
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", SEED_USER_ID);

        jdbcTemplate.update(
                "INSERT INTO users (id, email, nickname, role) VALUES (?, ?, ?, 'USER')",
                SEED_USER_ID, "page-size-clamp@gout.test", "클램프시드유저");

        // 테스트용 게시글 5건 삽입 (size 경계값 검증에 충분한 수)
        for (int i = 1; i <= 5; i++) {
            postRepository.save(Post.builder()
                    .userId(SEED_USER_ID)
                    .category(Post.PostCategory.FREE)
                    .title("클램프 테스트 게시글 " + i)
                    .content("내용 " + i)
                    .isAnonymous(false)
                    .build());
        }
    }

    @Test
    @DisplayName("size=100000 요청 시 실제 pageSize 는 100 이하여야 함")
    void oversized_request_is_clamped_to_100() throws Exception {
        JsonNode body = objectMapper.readTree(
                mockMvc.perform(get("/api/posts").param("size", "100000"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString());

        int pageSize = body.path("data").path("size").asInt();
        assertTrue(pageSize <= 100,
                "size=100000 요청에 대해 pageSize 가 100 이하여야 하지만 실제: " + pageSize);
    }

    @Test
    @DisplayName("size=-1 요청 시 pageSize 는 기본값 20 이어야 함")
    void negative_size_falls_back_to_default_20() throws Exception {
        JsonNode body = objectMapper.readTree(
                mockMvc.perform(get("/api/posts").param("size", "-1"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString());

        int pageSize = body.path("data").path("size").asInt();
        assertEquals(20, pageSize,
                "size=-1 요청에 대해 기본 pageSize 20 이어야 하지만 실제: " + pageSize);
    }

    @Test
    @DisplayName("size=50 정상 범위는 그대로 50 반환")
    void normal_size_passes_through() throws Exception {
        JsonNode body = objectMapper.readTree(
                mockMvc.perform(get("/api/posts").param("size", "50"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString());

        int pageSize = body.path("data").path("size").asInt();
        assertEquals(50, pageSize,
                "size=50 정상 요청에 대해 pageSize 50 이어야 하지만 실제: " + pageSize);
    }
}

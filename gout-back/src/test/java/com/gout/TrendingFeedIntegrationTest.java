package com.gout;

import com.gout.dao.PostLikeRepository;
import com.gout.dao.PostRepository;
import com.gout.entity.Post;
import com.gout.entity.PostLike;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GET /api/posts/trending?days=7&limit=5 통합 테스트.
 *
 * JdbcTemplate 로 users / posts 를 직접 seed 해 gender_type 버그를 회피한다.
 * PostSearchIntegrationTest 와 동일한 패턴을 따른다.
 */
class TrendingFeedIntegrationTest extends IntegrationTestBase {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostLikeRepository postLikeRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String SEED_USER_ID = "trending-seed-user-id";

    @BeforeEach
    void seedData() {
        jdbcTemplate.update("DELETE FROM post_likes");
        jdbcTemplate.update("DELETE FROM posts");
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", SEED_USER_ID);

        jdbcTemplate.update(
                "INSERT INTO users (id, email, nickname, role) VALUES (?, ?, ?, 'USER')",
                SEED_USER_ID, "trending-seed@gout.test", "트렌딩시드유저");
    }

    @Test
    @DisplayName("좋아요 수 내림차순 정렬 — 좋아요 많은 글이 먼저 노출")
    void trending_sorted_by_like_count_desc() throws Exception {
        Post low = postRepository.save(Post.builder()
                .userId(SEED_USER_ID)
                .category(Post.PostCategory.FREE)
                .title("좋아요 1개 글")
                .content("본문")
                .isAnonymous(false)
                .build());

        Post high = postRepository.save(Post.builder()
                .userId(SEED_USER_ID)
                .category(Post.PostCategory.FREE)
                .title("좋아요 3개 글")
                .content("본문")
                .isAnonymous(false)
                .build());

        // likeCount 는 toggleLike 가 엔티티를 통해 갱신하므로 직접 save 로 증가.
        postLikeRepository.save(PostLike.of(low.getId(), SEED_USER_ID + "-a"));
        low.toggleLike(true);
        postRepository.save(low);

        postLikeRepository.save(PostLike.of(high.getId(), SEED_USER_ID + "-a"));
        postLikeRepository.save(PostLike.of(high.getId(), SEED_USER_ID + "-b"));
        postLikeRepository.save(PostLike.of(high.getId(), SEED_USER_ID + "-c"));
        high.toggleLike(true);
        high.toggleLike(true);
        high.toggleLike(true);
        postRepository.save(high);

        JsonNode body = objectMapper.readTree(
                mockMvc.perform(get("/api/posts/trending").param("days", "7").param("limit", "5"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString());

        JsonNode data = body.path("data");
        assertTrue(data.isArray(), "data 는 배열이어야 함");
        assertTrue(data.size() >= 2, "2건 이상 반환돼야 함");

        // 첫 번째 글이 좋아요가 더 많아야 함
        int firstLike = data.get(0).path("likeCount").asInt();
        int secondLike = data.get(1).path("likeCount").asInt();
        assertTrue(firstLike >= secondLike, "좋아요 내림차순 정렬이어야 함");
        assertEquals("좋아요 3개 글", data.get(0).path("title").asText());
    }

    @Test
    @DisplayName("days=0 은 범위 밖 → 기본값 7로 clamp 처리, 200 반환")
    void trending_days_zero_returns_200_with_clamp() throws Exception {
        postRepository.save(Post.builder()
                .userId(SEED_USER_ID)
                .category(Post.PostCategory.FREE)
                .title("클램프 테스트 글")
                .content("본문")
                .isAnonymous(false)
                .build());

        mockMvc.perform(get("/api/posts/trending").param("days", "0").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("limit=50 은 범위 밖 → 20으로 clamp, 응답 건수 ≤ 20")
    void trending_limit_over_20_clamped_to_20() throws Exception {
        // 25건 적재
        for (int i = 0; i < 25; i++) {
            postRepository.save(Post.builder()
                    .userId(SEED_USER_ID)
                    .category(Post.PostCategory.FREE)
                    .title("글 " + i)
                    .content("본문")
                    .isAnonymous(false)
                    .build());
        }

        JsonNode body = objectMapper.readTree(
                mockMvc.perform(get("/api/posts/trending").param("days", "7").param("limit", "50"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString());

        int size = body.path("data").size();
        assertTrue(size <= 20, "clamp 후 최대 20건이어야 함, 실제=" + size);
    }
}

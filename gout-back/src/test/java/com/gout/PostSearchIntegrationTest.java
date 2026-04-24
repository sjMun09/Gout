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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GET /api/posts?keyword=... 검색 통합 테스트.
 *
 * <p>PostIntegrationTest 의 흐름 테스트는 registerAndLogin() 의 gender_type 버그로
 * @Disabled 상태다. 본 테스트는 인증 불필요한 GET 목록만 검증하므로,
 * JdbcTemplate 로 users / posts 에 직접 seed 해서 확인한다(User 엔티티의
 * gender enum 버그 회피).
 */
class PostSearchIntegrationTest extends IntegrationTestBase {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String SEED_USER_ID = "search-seed-user-id";

    @BeforeEach
    void seedPosts() {
        // 격리: 이전 테스트가 남긴 post/user 정리.
        // posts.user_id FK ON DELETE CASCADE 이므로 user 삭제로 posts 도 함께 정리됨.
        jdbcTemplate.update("DELETE FROM posts");
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", SEED_USER_ID);

        // users.gender 컬럼은 PG enum(gender_type). Hibernate 매핑 버그 회피 위해 NULL 로 둠.
        jdbcTemplate.update(
                "INSERT INTO users (id, email, nickname, role) VALUES (?, ?, ?, 'USER')",
                SEED_USER_ID, "search-seed@gout.test", "검색시드유저");

        postRepository.save(Post.builder()
                .userId(SEED_USER_ID)
                .category(Post.PostCategory.FREE)
                .title("통풍 식단 관리 팁")
                .content("하루 물 2L 이상 꼭 마시세요")
                .isAnonymous(false)
                .build());

        postRepository.save(Post.builder()
                .userId(SEED_USER_ID)
                .category(Post.PostCategory.FREE)
                .title("운동 후기")
                .content("통풍 환자도 걷기 유산소는 가능합니다")
                .isAnonymous(false)
                .build());

        postRepository.save(Post.builder()
                .userId(SEED_USER_ID)
                .category(Post.PostCategory.FREE)
                .title("병원 리뷰")
                .content("내용 없음")
                .isAnonymous(false)
                .build());

        // 삭제된 글은 검색 결과에 제외돼야 함
        Post deleted = postRepository.save(Post.builder()
                .userId(SEED_USER_ID)
                .category(Post.PostCategory.FREE)
                .title("삭제된 통풍 글")
                .content("삭제 대상")
                .isAnonymous(false)
                .build());
        deleted.delete();
        postRepository.save(deleted);
    }

    @Test
    @DisplayName("빈 키워드 → 삭제 제외한 모든 VISIBLE 게시글 반환")
    void post_search_empty_keyword_returns_all_visible() throws Exception {
        JsonNode body = objectMapper.readTree(
                mockMvc.perform(get("/api/posts"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.content").isArray())
                        .andReturn()
                        .getResponse()
                        .getContentAsString());

        int total = body.path("data").path("totalElements").asInt();
        assertEquals(3, total, "DELETED 상태 글은 제외하고 VISIBLE 3건만 반환돼야 함");

        // 삭제된 글 제목이 들어있지 않은지 확인
        boolean deletedLeaked = false;
        for (JsonNode item : body.path("data").path("content")) {
            if ("삭제된 통풍 글".equals(item.path("title").asText())) {
                deletedLeaked = true;
                break;
            }
        }
        assertTrue(!deletedLeaked, "DELETED 상태 글이 목록에 노출되면 안 됨");
    }

    @Test
    @DisplayName("키워드가 제목과 매칭")
    void post_search_by_keyword_title_hit() throws Exception {
        JsonNode body = objectMapper.readTree(
                mockMvc.perform(get("/api/posts").param("keyword", "운동"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString());

        assertEquals(1, body.path("data").path("totalElements").asInt());
        assertEquals("운동 후기",
                body.path("data").path("content").get(0).path("title").asText());
    }

    @Test
    @DisplayName("키워드가 본문과 매칭")
    void post_search_by_keyword_content_hit() throws Exception {
        // '걷기' 는 본문에만 있는 단어
        JsonNode body = objectMapper.readTree(
                mockMvc.perform(get("/api/posts").param("keyword", "걷기"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString());

        assertEquals(1, body.path("data").path("totalElements").asInt());
        assertEquals("운동 후기",
                body.path("data").path("content").get(0).path("title").asText());
    }

    @Test
    @DisplayName("키워드 대소문자 무시 (case-insensitive)")
    void post_search_by_keyword_case_insensitive() throws Exception {
        // 영문 포함 글 추가
        postRepository.save(Post.builder()
                .userId(SEED_USER_ID)
                .category(Post.PostCategory.FREE)
                .title("Uric Acid Management")
                .content("URIC acid level tips")
                .isAnonymous(false)
                .build());

        // 소문자 keyword 로 대문자 포함 제목/본문 매칭
        JsonNode lower = objectMapper.readTree(
                mockMvc.perform(get("/api/posts").param("keyword", "uric"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString());

        assertEquals(1, lower.path("data").path("totalElements").asInt());

        // 대문자 keyword 로 소문자 포함 단어 매칭 확인
        JsonNode upper = objectMapper.readTree(
                mockMvc.perform(get("/api/posts").param("keyword", "ACID"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString());

        assertEquals(1, upper.path("data").path("totalElements").asInt());
    }

    @Test
    @DisplayName("매칭되지 않는 키워드 → 빈 결과")
    void post_search_by_keyword_no_match() throws Exception {
        mockMvc.perform(get("/api/posts").param("keyword", "존재하지않는문자열ZZZ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(0));
    }
}

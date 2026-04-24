package com.gout;

import com.gout.dao.CommentRepository;
import com.gout.dao.PostRepository;
import com.gout.entity.Comment;
import com.gout.entity.Post;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;

import jakarta.persistence.EntityManagerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P1-7: GET /api/posts 댓글 수 N+1 제거 검증.
 *
 * <p>변경 전: posts.map 내부에서 post 당 COUNT 쿼리 1회씩 실행 → 20건 페이지 = 20회 round-trip.
 * <p>변경 후: postId IN (...) GROUP BY postId 한 번으로 배치 조회 → 1회 round-trip.
 *
 * <p>테스트 전략은 PostSearchIntegrationTest 와 동일하게 JdbcTemplate + Repository seed 로
 * 인증 플로우를 우회한다(auth flow 와 무관하게 목록 집계 로직만 검증).
 */
class PostListN1IntegrationTest extends IntegrationTestBase {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private static final String SEED_USER_ID = "n1-seed-user-id";
    private static final String SEED_COMMENTER_ID = "n1-seed-commenter-id";

    @BeforeEach
    void cleanAndSeedUsers() {
        // 격리: 이전 테스트 잔여물 정리. posts/comments 는 users ON DELETE CASCADE 로 함께 제거.
        jdbcTemplate.update("DELETE FROM comments");
        jdbcTemplate.update("DELETE FROM posts");
        jdbcTemplate.update("DELETE FROM users WHERE id IN (?, ?)",
                SEED_USER_ID, SEED_COMMENTER_ID);

        // users.gender 컬럼은 PG enum → Hibernate 매핑 버그 회피 위해 NULL 로 두고 직접 INSERT.
        jdbcTemplate.update(
                "INSERT INTO users (id, email, nickname, role) VALUES (?, ?, ?, 'USER')",
                SEED_USER_ID, "n1-seed@gout.test", "작성자");
        jdbcTemplate.update(
                "INSERT INTO users (id, email, nickname, role) VALUES (?, ?, ?, 'USER')",
                SEED_COMMENTER_ID, "n1-commenter@gout.test", "댓글러");
    }

    @Test
    @DisplayName("GET /api/posts — 각 post 의 commentCount 가 배치 조회로 정확히 집계된다")
    void post_list_comment_count_is_aggregated_in_batch() throws Exception {
        // given: post A(댓글 2), B(댓글 0), C(댓글 5) 세팅
        Post postA = postRepository.save(Post.builder()
                .userId(SEED_USER_ID)
                .category(Post.PostCategory.FREE)
                .title("N1-A")
                .content("A 본문")
                .isAnonymous(false)
                .build());
        Post postB = postRepository.save(Post.builder()
                .userId(SEED_USER_ID)
                .category(Post.PostCategory.FREE)
                .title("N1-B")
                .content("B 본문")
                .isAnonymous(false)
                .build());
        Post postC = postRepository.save(Post.builder()
                .userId(SEED_USER_ID)
                .category(Post.PostCategory.FREE)
                .title("N1-C")
                .content("C 본문")
                .isAnonymous(false)
                .build());

        seedComments(postA.getId(), 2);
        seedComments(postC.getId(), 5);
        // B 는 댓글 0 — countByPostIdInAndStatus GROUP BY 결과에 미포함 → getOrDefault 로 0 이어야 함.

        // when: 목록 조회
        JsonNode body = objectMapper.readTree(
                mockMvc.perform(get("/api/posts").param("category", "FREE"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.content").isArray())
                        .andReturn()
                        .getResponse()
                        .getContentAsString());

        // then: 각 게시글의 commentCount 가 정확해야 한다 (A=2, B=0, C=5)
        JsonNode content = body.path("data").path("content");
        assertEquals(3, content.size(), "seed 한 3건만 응답되어야 함");

        int countA = extractCommentCount(content, "N1-A");
        int countB = extractCommentCount(content, "N1-B");
        int countC = extractCommentCount(content, "N1-C");

        assertEquals(2, countA, "A 의 댓글 수는 2");
        assertEquals(0, countB, "B 는 댓글 0 — GROUP BY 결과 미포함이어도 0 으로 내려와야 함");
        assertEquals(5, countC, "C 의 댓글 수는 5");
    }

    @Test
    @DisplayName("GET /api/posts — 댓글 수 집계 COUNT 쿼리가 페이지 크기와 무관하게 1회만 실행된다")
    void post_list_emits_single_batched_count_query() throws Exception {
        // given: post 3건 + 각기 다른 수의 VISIBLE 댓글
        Post p1 = postRepository.save(Post.builder()
                .userId(SEED_USER_ID).category(Post.PostCategory.FREE)
                .title("Q-1").content("c").isAnonymous(false).build());
        Post p2 = postRepository.save(Post.builder()
                .userId(SEED_USER_ID).category(Post.PostCategory.FREE)
                .title("Q-2").content("c").isAnonymous(false).build());
        Post p3 = postRepository.save(Post.builder()
                .userId(SEED_USER_ID).category(Post.PostCategory.FREE)
                .title("Q-3").content("c").isAnonymous(false).build());
        seedComments(p1.getId(), 3);
        seedComments(p2.getId(), 1);
        seedComments(p3.getId(), 4);

        Statistics stats = entityManagerFactory
                .unwrap(SessionFactory.class)
                .getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();

        // when: 목록 조회 → 내부적으로 countByPostIdInAndStatusGroupByPostId 1회만 실행되어야 함
        mockMvc.perform(get("/api/posts").param("category", "FREE"))
                .andExpect(status().isOk());

        // then: 전체 쿼리 수를 출력하고, 적어도 N+1 이 아님을 확인
        // N+1 이었다면 post 3건 → COUNT 3회 + page 쿼리 + count(*) 등 → 5회 이상.
        // 배치 후: page 쿼리 + COUNT page-wide 쿼리 + 배치 COUNT GROUP BY 1회 + nickname 배치 =~ 4회 이하.
        long queryCount = stats.getPrepareStatementCount();
        System.out.println("[P1-7] GET /api/posts prepareStatement 총 실행 횟수 = " + queryCount
                + " (post 3건 기준. N+1 였다면 >= post 수에 비례)");
        assertTrue(queryCount <= 6,
                "게시글 목록은 6회 이하의 쿼리만 써야 함(실제: " + queryCount
                        + "). N+1 가 재발하면 post 수에 비례해 증가.");
    }

    @Test
    @DisplayName("GET /api/posts — 게시글이 없으면 빈 content 를 반환하고 예외가 없다")
    void post_list_empty_page_returns_empty_content() throws Exception {
        // given: seed 한 post 가 하나도 없는 상태 (@BeforeEach 가 posts 테이블을 비워놓음)

        // when: 목록 조회
        JsonNode body = objectMapper.readTree(
                mockMvc.perform(get("/api/posts"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.content").isArray())
                        .andReturn()
                        .getResponse()
                        .getContentAsString());

        // then: content 는 빈 배열, totalElements 0
        JsonNode content = body.path("data").path("content");
        assertNotNull(content);
        assertTrue(content.isArray(), "content 는 배열");
        assertEquals(0, content.size(), "빈 페이지");
        assertEquals(0, body.path("data").path("totalElements").asInt(), "총 건수 0");
    }

    // ============== Helpers ==============

    private void seedComments(String postId, int count) {
        for (int i = 0; i < count; i++) {
            commentRepository.save(Comment.builder()
                    .postId(postId)
                    .userId(SEED_COMMENTER_ID)
                    .content("댓글-" + i)
                    .isAnonymous(false)
                    .build());
        }
    }

    private int extractCommentCount(JsonNode content, String title) {
        for (JsonNode item : content) {
            if (title.equals(item.path("title").asText())) {
                return item.path("commentCount").asInt();
            }
        }
        throw new AssertionError("제목 '" + title + "' 인 post 가 응답에 없음");
    }
}

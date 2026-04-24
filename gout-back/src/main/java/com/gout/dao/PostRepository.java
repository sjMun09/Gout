package com.gout.dao;

import com.gout.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, String> {

    /**
     * 조회수 아토믹 증가. dirty-checking UPDATE 는 lost-update 에 취약하므로
     * 조회 엔드포인트에서는 이 쿼리를 사용한다.
     *
     * <p>반환값은 영향받은 row 수 — 0 이면 존재하지 않거나 삭제된 게시글.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Post p SET p.viewCount = p.viewCount + 1 WHERE p.id = :id AND p.status = 'VISIBLE'")
    int incrementViewCount(@Param("id") String id);

    @Query("SELECT p FROM Post p WHERE p.status = 'VISIBLE' " +
           "AND (:category IS NULL OR p.category = :category) " +
           "ORDER BY p.createdAt DESC")
    Page<Post> findVisible(@Param("category") Post.PostCategory category, Pageable pageable);

    /**
     * 카테고리 + 키워드(제목/본문) 복합 필터. status='DELETED' 는 제외.
     * <p>
     * keyword 는 서비스 계층에서 비-null(빈 문자열로 coalesce)로 전달해야 한다.
     * null 을 바인딩하면 Postgres JDBC 드라이버가 LOWER 의 인자 타입을 bytea 로
     * 추론해 500 을 뱉는 이슈가 있어(FoodRepository 의 동일 이슈 주석 참고),
     * "LENGTH(:keyword) = 0 OR ..." 조건으로 회피한다.
     */
    /**
     * 정렬은 Pageable 의 Sort 로 동적 제어 (latest / popular / views).
     * 서비스 계층에서 Sort 를 구성해 넘기므로 쿼리 내 ORDER BY 는 제거.
     */
    @Query("SELECT p FROM Post p WHERE p.status = 'VISIBLE' " +
           "AND (:category IS NULL OR p.category = :category) " +
           "AND (LENGTH(:keyword) = 0 " +
           "     OR LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "     OR LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Post> searchVisible(@Param("category") Post.PostCategory category,
                             @Param("keyword") String keyword,
                             Pageable pageable);

    /**
     * 기간 내 VISIBLE 게시글을 좋아요 수 내림차순으로 반환.
     * 동일 좋아요 수면 최신글 우선(createdAt DESC)으로 tie-break.
     * Pageable 로 limit 을 제어하므로 호출측에서 PageRequest.of(0, limit) 을 넘긴다.
     */
    @Query("SELECT p FROM Post p WHERE p.status = 'VISIBLE' AND p.createdAt >= :since " +
           "ORDER BY p.likeCount DESC, p.createdAt DESC")
    List<Post> findTrending(@Param("since") java.time.LocalDateTime since, Pageable pageable);
}

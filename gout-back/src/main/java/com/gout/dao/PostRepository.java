package com.gout.dao;

import com.gout.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, String> {

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
    @Query("SELECT p FROM Post p WHERE p.status = 'VISIBLE' " +
           "AND (:category IS NULL OR p.category = :category) " +
           "AND (LENGTH(:keyword) = 0 " +
           "     OR LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "     OR LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY p.createdAt DESC")
    Page<Post> searchVisible(@Param("category") Post.PostCategory category,
                             @Param("keyword") String keyword,
                             Pageable pageable);
}

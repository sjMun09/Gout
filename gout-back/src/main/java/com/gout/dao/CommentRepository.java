package com.gout.dao;

import com.gout.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, String> {

    List<Comment> findByPostIdAndStatusOrderByCreatedAtAsc(String postId, Comment.Status status);

    long countByPostIdAndStatus(String postId, Comment.Status status);

    /**
     * P1-7: 게시글 목록 댓글 수 집계용 배치 조회.
     * postId IN (:postIds) GROUP BY postId 로 N+1 COUNT 를 1회 GROUP BY 쿼리로 치환한다.
     * 댓글이 0개인 post 는 결과에 포함되지 않으므로 호출부에서 getOrDefault(..., 0L) 로 처리.
     */
    @Query("SELECT c.postId AS postId, COUNT(c) AS cnt FROM Comment c "
            + "WHERE c.postId IN :postIds AND c.status = :status GROUP BY c.postId")
    List<PostCommentCount> countByPostIdInAndStatusGroupByPostId(
            @Param("postIds") Collection<String> postIds,
            @Param("status") Comment.Status status);

    /** Spring Data JPA projection — DTO 없이 GROUP BY 결과를 매핑. */
    interface PostCommentCount {
        String getPostId();
        long getCnt();
    }
}

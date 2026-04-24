package com.gout.dao;

import com.gout.entity.PostBookmark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PostBookmarkRepository extends JpaRepository<PostBookmark, String> {

    Optional<PostBookmark> findByUserIdAndPostId(String userId, String postId);

    boolean existsByUserIdAndPostId(String userId, String postId);

    long countByPostId(String postId);

    Page<PostBookmark> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    /**
     * 유저 탈퇴 시 해당 사용자의 모든 북마크를 물리 삭제 (Right to be forgotten).
     */
    @Modifying
    @Query("DELETE FROM PostBookmark b WHERE b.userId = :userId")
    int deleteByUserId(@Param("userId") String userId);
}

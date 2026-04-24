package com.gout.dao;

import com.gout.entity.PostBookmark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostBookmarkRepository extends JpaRepository<PostBookmark, String> {

    Optional<PostBookmark> findByUserIdAndPostId(String userId, String postId);

    boolean existsByUserIdAndPostId(String userId, String postId);

    long countByPostId(String postId);

    Page<PostBookmark> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
}

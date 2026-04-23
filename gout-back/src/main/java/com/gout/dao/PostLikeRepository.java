package com.gout.dao;

import com.gout.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostLikeRepository extends JpaRepository<PostLike, PostLike.PostLikeId> {

    boolean existsByIdPostIdAndIdUserId(String postId, String userId);

    void deleteByIdPostIdAndIdUserId(String postId, String userId);
}

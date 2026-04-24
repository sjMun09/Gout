package com.gout.dao;

import com.gout.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostLikeRepository extends JpaRepository<PostLike, PostLike.PostLikeId> {

    boolean existsByIdPostIdAndIdUserId(String postId, String userId);

    void deleteByIdPostIdAndIdUserId(String postId, String userId);

    /**
     * 유저 탈퇴 시 해당 사용자의 모든 좋아요를 물리 삭제한다.
     * FK CASCADE 를 대체하는 앱 레벨 cascade (Right to be forgotten).
     */
    @Modifying
    @Query("DELETE FROM PostLike pl WHERE pl.id.userId = :userId")
    int deleteByUserId(@Param("userId") String userId);
}

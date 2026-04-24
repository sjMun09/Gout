package com.gout.dao;

import com.gout.entity.PostHashtag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface PostHashtagRepository extends JpaRepository<PostHashtag, Long> {

    /** 게시글 목록 조회 시 배치 IN 쿼리 — N+1 방지 */
    List<PostHashtag> findByPostIdIn(Collection<String> postIds);

    /** 게시글 상세 조회 */
    List<PostHashtag> findByPostId(String postId);

    /** 수정/삭제 시 기존 태그 전체 제거 */
    @Modifying
    @Query("DELETE FROM PostHashtag p WHERE p.postId = :postId")
    void deleteAllByPostId(@Param("postId") String postId);

    /** 태그로 게시글 ID 조회 (태그 필터) */
    @Query("SELECT DISTINCT h.postId FROM PostHashtag h WHERE h.tag = :tag")
    Set<String> findPostIdsByTag(@Param("tag") String tag);
}

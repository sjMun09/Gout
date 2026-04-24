package com.gout.dao;

import com.gout.entity.GoutAttackLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GoutAttackLogRepository extends JpaRepository<GoutAttackLog, String> {

    List<GoutAttackLog> findByUserIdOrderByAttackedAtDesc(String userId);

    /** 유저 탈퇴 시 개인 건강 기록 물리 삭제 (GDPR). */
    @Modifying
    @Query("DELETE FROM GoutAttackLog l WHERE l.userId = :userId")
    int deleteByUserId(@Param("userId") String userId);
}

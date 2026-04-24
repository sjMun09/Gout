package com.gout.dao;

import com.gout.entity.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReportRepository extends JpaRepository<Report, String> {

    Page<Report> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    long countByTargetTypeAndTargetId(String targetType, String targetId);

    boolean existsByTargetTypeAndTargetIdAndReporterId(String targetType,
                                                      String targetId,
                                                      String reporterId);

    /**
     * 유저 탈퇴 시 해당 사용자가 제기한 신고의 reporter_id 를 NULL 로 치환.
     * 신고 기록 자체는 감사 로그 목적으로 유지.
     */
    @Modifying
    @Query("UPDATE Report r SET r.reporterId = NULL WHERE r.reporterId = :userId")
    int anonymizeByReporterId(@Param("userId") String userId);
}

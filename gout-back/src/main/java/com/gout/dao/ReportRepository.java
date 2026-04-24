package com.gout.dao;

import com.gout.entity.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, String> {

    Page<Report> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    long countByTargetTypeAndTargetId(String targetType, String targetId);

    boolean existsByTargetTypeAndTargetIdAndReporterId(String targetType,
                                                      String targetId,
                                                      String reporterId);
}

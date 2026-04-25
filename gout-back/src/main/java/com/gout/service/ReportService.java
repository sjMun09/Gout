package com.gout.service;

import com.gout.dto.request.CreateReportRequest;
import com.gout.dto.response.ReportResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReportService {

    ReportResponse create(String reporterId, CreateReportRequest request);

    /**
     * 관리자용 신고 목록. status (PENDING/RESOLVED/DISMISSED) 로 필터링, createdAt DESC.
     */
    Page<ReportResponse> listForAdmin(String status, Pageable pageable);

    /** 관리자가 신고를 처리 완료(RESOLVED)로 표시. */
    void resolve(String reportId);

    /** 관리자가 신고를 기각(DISMISSED)으로 표시. */
    void dismiss(String reportId);
}

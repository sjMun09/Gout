package com.gout.service.impl;

import com.gout.dao.CommentRepository;
import com.gout.dao.PostRepository;
import com.gout.dao.ReportRepository;
import com.gout.dto.request.CreateReportRequest;
import com.gout.dto.response.ReportResponse;
import com.gout.entity.Report;
import com.gout.global.exception.BusinessException;
import com.gout.global.exception.ErrorCode;
import com.gout.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    @Override
    @Transactional
    public ReportResponse create(String reporterId, CreateReportRequest request) {
        if (!Report.TargetType.isValid(request.getTargetType())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "targetType 은 POST 또는 COMMENT 여야 합니다.");
        }
        if (!Report.Reason.isValid(request.getReason())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "reason 은 SPAM/ABUSE/SEXUAL/MISINFO/ETC 중 하나여야 합니다.");
        }

        String targetType = request.getTargetType();
        String targetId = request.getTargetId();

        // 대상 존재 여부 확인
        if (Report.TargetType.POST.name().equals(targetType)) {
            if (!postRepository.existsById(targetId)) {
                throw new BusinessException(ErrorCode.POST_NOT_FOUND);
            }
        } else { // COMMENT
            if (!commentRepository.existsById(targetId)) {
                throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND);
            }
        }

        // 중복 신고 방지 (사전 체크 — 경쟁 상황은 DB UNIQUE 제약이 최종 방어)
        if (reportRepository.existsByTargetTypeAndTargetIdAndReporterId(
                targetType, targetId, reporterId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_REPORT);
        }

        Report report = Report.builder()
                .targetType(targetType)
                .targetId(targetId)
                .reporterId(reporterId)
                .reason(request.getReason())
                .detail(request.getDetail())
                .build();

        try {
            Report saved = reportRepository.saveAndFlush(report);
            return ReportResponse.of(saved);
        } catch (DataIntegrityViolationException e) {
            // UNIQUE (target_type, target_id, reporter_id) 경쟁 — 409 로 응답
            throw new BusinessException(ErrorCode.DUPLICATE_REPORT);
        }
    }

    private static final Set<String> VALID_STATUSES = Set.of("PENDING", "RESOLVED", "DISMISSED");

    @Override
    @Transactional(readOnly = true)
    public Page<ReportResponse> listForAdmin(String status, Pageable pageable) {
        if (status == null || !VALID_STATUSES.contains(status)) {
            throw new BusinessException(ErrorCode.INVALID_REPORT_STATUS);
        }
        return reportRepository.findByStatusOrderByCreatedAtDesc(status, pageable)
                .map(ReportResponse::of);
    }

    @Override
    @Transactional
    public void resolve(String reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REPORT_NOT_FOUND));
        report.resolve();
    }

    @Override
    @Transactional
    public void dismiss(String reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REPORT_NOT_FOUND));
        report.dismiss();
    }
}

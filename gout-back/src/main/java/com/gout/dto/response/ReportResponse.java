package com.gout.dto.response;

import com.gout.entity.Report;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ReportResponse {

    private final String id;
    private final String targetType;
    private final String targetId;
    private final String reporterId;
    private final String reason;
    private final String detail;
    private final String status;
    private final LocalDateTime createdAt;
    private final LocalDateTime resolvedAt;

    public static ReportResponse of(Report r) {
        return ReportResponse.builder()
                .id(r.getId())
                .targetType(r.getTargetType())
                .targetId(r.getTargetId())
                .reporterId(r.getReporterId())
                .reason(r.getReason())
                .detail(r.getDetail())
                .status(r.getStatus() != null ? r.getStatus().name() : null)
                .createdAt(r.getCreatedAt())
                .resolvedAt(r.getResolvedAt())
                .build();
    }
}

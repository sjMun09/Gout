package com.gout.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 신고 생성 요청.
 * targetType / reason 의 허용값 검증은 서비스 레이어에서 Report.TargetType / Reason 으로 수행.
 * (enum 으로 바인딩하면 400 메시지가 jackson 기본값이 되어 안내가 모호해지는 문제를 회피)
 */
@Getter
@Setter
public class CreateReportRequest {

    @NotBlank(message = "targetType 은 필수입니다.")
    private String targetType;

    @NotBlank(message = "targetId 는 필수입니다.")
    private String targetId;

    @NotBlank(message = "reason 은 필수입니다.")
    private String reason;

    @Size(max = 2000, message = "detail 은 최대 2000자까지 입력 가능합니다.")
    private String detail;
}

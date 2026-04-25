package com.gout.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "요산수치 측정 로그 생성 요청.")
@Getter
public class CreateUricAcidLogRequest {

    @Schema(description = "요산수치(mg/dL).", example = "6.5", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "요산 수치는 필수입니다.")
    private BigDecimal value;

    @Schema(description = "측정일(yyyy-MM-dd).", example = "2026-04-26", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "측정일은 필수입니다.")
    private LocalDate measuredAt;

    @Schema(description = "메모(최대 500자, 선택).", example = "공복 측정")
    @Size(max = 500, message = "메모는 최대 500자까지 입력 가능합니다.")
    private String memo;
}

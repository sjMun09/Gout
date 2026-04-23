package com.gout.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
public class CreateUricAcidLogRequest {

    @NotNull(message = "요산 수치는 필수입니다.")
    private BigDecimal value;

    @NotNull(message = "측정일은 필수입니다.")
    private LocalDate measuredAt;

    @Size(max = 500, message = "메모는 최대 500자까지 입력 가능합니다.")
    private String memo;
}

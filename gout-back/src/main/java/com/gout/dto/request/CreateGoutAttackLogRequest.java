package com.gout.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class CreateGoutAttackLogRequest {

    @NotNull(message = "발작 발생일은 필수입니다.")
    private LocalDate attackedAt;

    @Min(value = 1, message = "통증 강도는 1 이상이어야 합니다.")
    @Max(value = 10, message = "통증 강도는 10 이하여야 합니다.")
    private Short painLevel;

    @Size(max = 100, message = "발작 부위는 최대 100자까지 입력 가능합니다.")
    private String location;

    private Short durationDays;

    @Size(max = 500, message = "추정 원인은 최대 500자까지 입력 가능합니다.")
    private String suspectedCause;

    private String memo;
}

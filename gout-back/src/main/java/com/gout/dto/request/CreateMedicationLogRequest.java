package com.gout.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class CreateMedicationLogRequest {

    @NotBlank(message = "약 이름은 필수입니다.")
    @Size(max = 200, message = "약 이름은 최대 200자까지 입력 가능합니다.")
    private String medicationName;

    @Size(max = 100, message = "복용량은 최대 100자까지 입력 가능합니다.")
    private String dosage;

    @NotNull(message = "복용 시각은 필수입니다.")
    private LocalDateTime takenAt;
}

package com.gout.dto.response;

import com.gout.entity.MedicationLog;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class MedicationLogResponse {

    private final String id;
    private final String medicationName;
    private final String dosage;
    private final LocalDateTime takenAt;
    private final LocalDateTime createdAt;

    private MedicationLogResponse(String id,
                                  String medicationName,
                                  String dosage,
                                  LocalDateTime takenAt,
                                  LocalDateTime createdAt) {
        this.id = id;
        this.medicationName = medicationName;
        this.dosage = dosage;
        this.takenAt = takenAt;
        this.createdAt = createdAt;
    }

    public static MedicationLogResponse of(MedicationLog log) {
        return new MedicationLogResponse(
                log.getId(),
                log.getMedicationName(),
                log.getDosage(),
                log.getTakenAt(),
                log.getCreatedAt()
        );
    }
}

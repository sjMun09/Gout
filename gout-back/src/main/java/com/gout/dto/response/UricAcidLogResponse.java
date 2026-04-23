package com.gout.dto.response;

import com.gout.entity.UricAcidLog;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
public class UricAcidLogResponse {

    private final String id;
    private final BigDecimal value;
    private final LocalDate measuredAt;
    private final String memo;
    private final LocalDateTime createdAt;

    private UricAcidLogResponse(String id,
                                BigDecimal value,
                                LocalDate measuredAt,
                                String memo,
                                LocalDateTime createdAt) {
        this.id = id;
        this.value = value;
        this.measuredAt = measuredAt;
        this.memo = memo;
        this.createdAt = createdAt;
    }

    public static UricAcidLogResponse of(UricAcidLog log) {
        return new UricAcidLogResponse(
                log.getId(),
                log.getValue(),
                log.getMeasuredAt(),
                log.getMemo(),
                log.getCreatedAt()
        );
    }
}

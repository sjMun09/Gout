package com.gout.dto.response;

import com.gout.entity.GoutAttackLog;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
public class GoutAttackLogResponse {

    private final String id;
    private final LocalDate attackedAt;
    private final Short painLevel;
    private final String location;
    private final Short durationDays;
    private final String suspectedCause;
    private final String memo;
    private final LocalDateTime createdAt;

    private GoutAttackLogResponse(String id,
                                  LocalDate attackedAt,
                                  Short painLevel,
                                  String location,
                                  Short durationDays,
                                  String suspectedCause,
                                  String memo,
                                  LocalDateTime createdAt) {
        this.id = id;
        this.attackedAt = attackedAt;
        this.painLevel = painLevel;
        this.location = location;
        this.durationDays = durationDays;
        this.suspectedCause = suspectedCause;
        this.memo = memo;
        this.createdAt = createdAt;
    }

    public static GoutAttackLogResponse of(GoutAttackLog log) {
        return new GoutAttackLogResponse(
                log.getId(),
                log.getAttackedAt(),
                log.getPainLevel(),
                log.getLocation(),
                log.getDurationDays(),
                log.getSuspectedCause(),
                log.getMemo(),
                log.getCreatedAt()
        );
    }
}

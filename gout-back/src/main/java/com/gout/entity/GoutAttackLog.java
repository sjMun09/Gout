package com.gout.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "gout_attack_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GoutAttackLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "attacked_at", nullable = false)
    private LocalDate attackedAt;

    @Column(name = "pain_level")
    private Short painLevel;

    @Column(length = 100)
    private String location;

    @Column(name = "duration_days")
    private Short durationDays;

    @Column(name = "suspected_cause", length = 500)
    private String suspectedCause;

    @Column(columnDefinition = "TEXT")
    private String memo;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private GoutAttackLog(String userId,
                          LocalDate attackedAt,
                          Short painLevel,
                          String location,
                          Short durationDays,
                          String suspectedCause,
                          String memo) {
        this.userId = userId;
        this.attackedAt = attackedAt;
        this.painLevel = painLevel;
        this.location = location;
        this.durationDays = durationDays;
        this.suspectedCause = suspectedCause;
        this.memo = memo;
    }

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}

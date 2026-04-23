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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "uric_acid_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UricAcidLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(nullable = false, precision = 4, scale = 1)
    private BigDecimal value;

    @Column(name = "measured_at", nullable = false)
    private LocalDate measuredAt;

    @Column(length = 500)
    private String memo;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private UricAcidLog(String userId, BigDecimal value, LocalDate measuredAt, String memo) {
        this.userId = userId;
        this.value = value;
        this.measuredAt = measuredAt;
        this.memo = memo;
    }

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}

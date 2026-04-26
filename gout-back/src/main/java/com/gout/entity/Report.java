package com.gout.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 게시글/댓글 신고 엔티티.
 * target_type / reason 은 문자열로 유지해 추후 값 추가 시 DB 스키마 변경이 필요 없도록 한다.
 * Java 측에서만 enum(TargetType / Reason) 으로 검증한다.
 */
@Entity
@Table(name = "reports")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "target_type", nullable = false, length = 20)
    private String targetType;

    @Column(name = "target_id", nullable = false, length = 36)
    private String targetId;

    // V24 이후 NULL 허용 — 신고자 탈퇴 시 감사 로그로서 신고 내역은 유지하되 신고자는 익명화.
    @Column(name = "reporter_id", length = 36)
    private String reporterId;

    @Column(nullable = false, length = 50)
    private String reason;

    @Column(columnDefinition = "TEXT")
    private String detail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Builder
    private Report(String targetType, String targetId, String reporterId,
                   String reason, String detail) {
        this.targetType = targetType;
        this.targetId = targetId;
        this.reporterId = reporterId;
        this.reason = reason;
        this.detail = detail;
        this.status = Status.PENDING;
    }

    @PrePersist
    void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = Status.PENDING;
        }
    }

    public void resolve() {
        this.status = Status.RESOLVED;
        this.resolvedAt = LocalDateTime.now();
    }

    public void dismiss() {
        this.status = Status.DISMISSED;
        this.resolvedAt = LocalDateTime.now();
    }

    /**
     * 신고 처리 상태. DB 컬럼은 VARCHAR(20) — EnumType.STRING 으로 enum.name() 보존.
     */
    public enum Status {
        PENDING, RESOLVED, DISMISSED;

        public static boolean isValid(String value) {
            if (value == null) return false;
            for (Status s : values()) {
                if (s.name().equals(value)) return true;
            }
            return false;
        }
    }

    /** 대상 타입 — DB 는 문자열, 검증은 Java 측에서. */
    public enum TargetType {
        POST, COMMENT;

        public static boolean isValid(String value) {
            if (value == null) return false;
            for (TargetType t : values()) {
                if (t.name().equals(value)) return true;
            }
            return false;
        }
    }

    /** 신고 사유 — DB 는 문자열, 검증은 Java 측에서. */
    public enum Reason {
        SPAM, ABUSE, SEXUAL, MISINFO, ETC;

        public static boolean isValid(String value) {
            if (value == null) return false;
            for (Reason r : values()) {
                if (r.name().equals(value)) return true;
            }
            return false;
        }
    }
}

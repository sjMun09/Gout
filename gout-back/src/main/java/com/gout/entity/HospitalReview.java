package com.gout.entity;

import com.gout.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "hospital_reviews")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HospitalReview extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "hospital_id", nullable = false)
    private String hospitalId;

    // V24 이후 NULL 허용 — 유저 탈퇴 시 리뷰 콘텐츠는 유지하고 작성자만 익명화.
    @Column(name = "user_id")
    private String userId;

    @Column(nullable = false)
    private Short rating;

    @Column(name = "category", nullable = false, length = 30)
    private String category;

    private String content;

    @Column(name = "visit_date")
    private LocalDate visitDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Builder
    private HospitalReview(String hospitalId,
                           String userId,
                           Short rating,
                           String category,
                           String content,
                           LocalDate visitDate,
                           Status status) {
        this.hospitalId = hospitalId;
        this.userId = userId;
        this.rating = rating;
        this.category = category != null ? category : "GENERAL";
        this.content = content;
        this.visitDate = visitDate;
        this.status = status != null ? status : Status.VISIBLE;
    }

    public void updateContent(Short rating, String category, String content) {
        if (rating != null) {
            this.rating = rating;
        }
        if (category != null) {
            this.category = category;
        }
        this.content = content;
    }

    public void hide() {
        this.status = Status.HIDDEN;
    }

    /**
     * 리뷰 상태. DB 컬럼은 VARCHAR(20) — EnumType.STRING 으로 enum.name() 보존.
     * V4 스키마 주석: VISIBLE / HIDDEN / REPORTED.
     */
    public enum Status {
        VISIBLE, HIDDEN, REPORTED
    }
}

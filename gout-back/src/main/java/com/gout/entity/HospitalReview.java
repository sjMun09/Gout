package com.gout.entity;

import com.gout.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

    @Column(nullable = false)
    private String status;

    @Builder
    private HospitalReview(String hospitalId,
                           String userId,
                           Short rating,
                           String category,
                           String content,
                           LocalDate visitDate,
                           String status) {
        this.hospitalId = hospitalId;
        this.userId = userId;
        this.rating = rating;
        this.category = category != null ? category : "GENERAL";
        this.content = content;
        this.visitDate = visitDate;
        this.status = status != null ? status : "VISIBLE";
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
        this.status = "HIDDEN";
    }
}

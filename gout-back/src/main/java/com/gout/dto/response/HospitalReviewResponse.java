package com.gout.dto.response;

import com.gout.entity.HospitalReview;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
public class HospitalReviewResponse {

    private final String id;
    private final String hospitalId;
    private final Short rating;
    private final String category;
    private final String content;
    private final LocalDate visitDate;
    private final LocalDateTime createdAt;

    private HospitalReviewResponse(String id,
                                   String hospitalId,
                                   Short rating,
                                   String category,
                                   String content,
                                   LocalDate visitDate,
                                   LocalDateTime createdAt) {
        this.id = id;
        this.hospitalId = hospitalId;
        this.rating = rating;
        this.category = category;
        this.content = content;
        this.visitDate = visitDate;
        this.createdAt = createdAt;
    }

    public static HospitalReviewResponse of(HospitalReview r) {
        return new HospitalReviewResponse(
                r.getId(),
                r.getHospitalId(),
                r.getRating(),
                r.getCategory(),
                r.getContent(),
                r.getVisitDate(),
                r.getCreatedAt()
        );
    }
}

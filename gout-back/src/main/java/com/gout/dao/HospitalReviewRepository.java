package com.gout.dao;

import com.gout.entity.HospitalReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface HospitalReviewRepository extends JpaRepository<HospitalReview, String> {

    Page<HospitalReview> findByHospitalIdAndStatus(String hospitalId, HospitalReview.Status status, Pageable pageable);

    boolean existsByUserIdAndHospitalIdAndVisitDate(String userId, String hospitalId, LocalDate visitDate);

    /**
     * 유저 탈퇴 시 해당 사용자의 리뷰 user_id 를 NULL 로 치환 (리뷰 콘텐츠는 유지).
     * 프론트에서는 작성자를 "탈퇴한 사용자" 로 표시.
     */
    @Modifying
    @Query("UPDATE HospitalReview r SET r.userId = NULL WHERE r.userId = :userId")
    int anonymizeByUserId(@Param("userId") String userId);
}

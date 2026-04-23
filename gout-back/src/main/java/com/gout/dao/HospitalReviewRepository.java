package com.gout.dao;

import com.gout.entity.HospitalReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface HospitalReviewRepository extends JpaRepository<HospitalReview, String> {

    Page<HospitalReview> findByHospitalIdAndStatus(String hospitalId, String status, Pageable pageable);

    boolean existsByUserIdAndHospitalIdAndVisitDate(String userId, String hospitalId, LocalDate visitDate);
}

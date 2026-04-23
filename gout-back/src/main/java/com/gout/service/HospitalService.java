package com.gout.service;

import com.gout.dto.request.CreateReviewRequest;
import com.gout.dto.request.HospitalSearchRequest;
import com.gout.dto.response.HospitalResponse;
import com.gout.dto.response.HospitalReviewResponse;
import org.springframework.data.domain.Page;

public interface HospitalService {

    Page<HospitalResponse> search(HospitalSearchRequest request);

    HospitalResponse findById(String id);

    HospitalReviewResponse createReview(String hospitalId, String userId, CreateReviewRequest request);

    Page<HospitalReviewResponse> getReviews(String hospitalId, int page, int size);
}

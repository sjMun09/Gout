package com.gout.controller;

import com.gout.dto.request.CreateReviewRequest;
import com.gout.dto.request.HospitalSearchRequest;
import com.gout.dto.response.HospitalResponse;
import com.gout.dto.response.HospitalReviewResponse;
import com.gout.global.exception.BusinessException;
import com.gout.global.exception.ErrorCode;
import com.gout.global.response.ApiResponse;
import com.gout.service.HospitalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hospitals")
@RequiredArgsConstructor
public class HospitalController {

    private final HospitalService hospitalService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<HospitalResponse>>> search(
            @ModelAttribute HospitalSearchRequest request) {
        return ResponseEntity.ok(ApiResponse.success(hospitalService.search(request)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<HospitalResponse>> detail(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(hospitalService.findById(id)));
    }

    @GetMapping("/{id}/reviews")
    public ResponseEntity<ApiResponse<Page<HospitalReviewResponse>>> reviews(
            @PathVariable String id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(hospitalService.getReviews(id, page, size)));
    }

    @PostMapping("/{id}/reviews")
    public ResponseEntity<ApiResponse<HospitalReviewResponse>> createReview(
            @PathVariable String id,
            @Valid @RequestBody CreateReviewRequest request) {
        String userId = currentUserId();
        return ResponseEntity.ok(
                ApiResponse.success("리뷰 작성 성공", hospitalService.createReview(id, userId, request)));
    }

    private String currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return auth.getName();
    }
}

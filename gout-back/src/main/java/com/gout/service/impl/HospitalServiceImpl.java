package com.gout.service.impl;

import com.gout.dao.HospitalRepository;
import com.gout.dao.HospitalReviewRepository;
import com.gout.dto.request.CreateReviewRequest;
import com.gout.dto.request.HospitalSearchRequest;
import com.gout.dto.response.HospitalResponse;
import com.gout.dto.response.HospitalReviewResponse;
import com.gout.entity.Hospital;
import com.gout.entity.HospitalReview;
import com.gout.global.exception.BusinessException;
import com.gout.global.exception.ErrorCode;
import com.gout.service.HospitalService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Array;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class HospitalServiceImpl implements HospitalService {

    private final HospitalRepository hospitalRepository;
    private final HospitalReviewRepository hospitalReviewRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<HospitalResponse> search(HospitalSearchRequest request) {
        int size = Math.max(1, request.getSize());
        int page = Math.max(0, request.getPage());
        Pageable pageable = PageRequest.of(page, size);
        // PG JDBC 는 JPQL 의 :keyword IS NULL 비교를 bytea 로 추론해 500 을 낸다.
        // 드라이버 버그를 우회하기 위해 null → "" 로 coalesce 하고 쿼리에서 LENGTH=0 으로 분기.
        String keyword = request.getKeyword() == null ? "" : request.getKeyword();

        if (request.hasLocation()) {
            List<Object[]> rows = hospitalRepository.searchByLocation(
                    request.getLat(),
                    request.getLng(),
                    request.getRadius(),
                    keyword.isEmpty() ? null : keyword,
                    size,
                    request.getOffset()
            );
            long total = hospitalRepository.countByLocation(
                    request.getLat(),
                    request.getLng(),
                    request.getRadius(),
                    keyword.isEmpty() ? null : keyword
            );
            List<HospitalResponse> content = rows.stream()
                    .map(this::mapNativeRow)
                    .toList();
            return new PageImpl<>(content, pageable, total);
        }

        Pageable pageableWithSort = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        Page<Hospital> hospitals = hospitalRepository.searchByKeyword(keyword, pageableWithSort);
        return hospitals.map(h -> HospitalResponse.of(h, null));
    }

    @Override
    @Transactional(readOnly = true)
    public HospitalResponse findById(String id) {
        Hospital hospital = hospitalRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.HOSPITAL_NOT_FOUND));
        return HospitalResponse.of(hospital, null);
    }

    @Override
    public HospitalReviewResponse createReview(String hospitalId, String userId, CreateReviewRequest request) {
        if (!hospitalRepository.existsById(hospitalId)) {
            throw new BusinessException(ErrorCode.HOSPITAL_NOT_FOUND);
        }

        if (request.getVisitDate() != null
                && hospitalReviewRepository.existsByUserIdAndHospitalIdAndVisitDate(
                        userId, hospitalId, request.getVisitDate())) {
            throw new BusinessException(ErrorCode.DUPLICATE_REVIEW);
        }

        HospitalReview review = HospitalReview.builder()
                .hospitalId(hospitalId)
                .userId(userId)
                .rating(request.getRating())
                .category(request.getCategory())
                .content(request.getContent())
                .visitDate(request.getVisitDate())
                .status("VISIBLE")
                .build();

        HospitalReview saved = hospitalReviewRepository.save(review);
        return HospitalReviewResponse.of(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<HospitalReviewResponse> getReviews(String hospitalId, int page, int size) {
        if (!hospitalRepository.existsById(hospitalId)) {
            throw new BusinessException(ErrorCode.HOSPITAL_NOT_FOUND);
        }
        Pageable pageable = PageRequest.of(
                Math.max(0, page),
                Math.max(1, size),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return hospitalReviewRepository.findByHospitalIdAndStatus(hospitalId, "VISIBLE", pageable)
                .map(HospitalReviewResponse::of);
    }

    /**
     * 네이티브 위치 검색 결과 매핑.
     * 컬럼 순서: id, hira_code, name, address, phone, is_active, latitude, longitude,
     *            departments, operating_hours, distance_meters
     */
    private HospitalResponse mapNativeRow(Object[] row) {
        String id = asString(row[0]);
        String name = asString(row[2]);
        String address = asString(row[3]);
        String phone = asString(row[4]);
        Double latitude = asDouble(row[6]);
        Double longitude = asDouble(row[7]);
        List<String> departments = asStringList(row[8]);
        String operatingHours = asString(row[9]);
        Double distanceMeters = asDouble(row[10]);

        return HospitalResponse.fromNative(
                id, name, address, phone,
                latitude, longitude,
                departments, operatingHours, distanceMeters
        );
    }

    private String asString(Object v) {
        return v == null ? null : v.toString();
    }

    private Double asDouble(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.doubleValue();
        return Double.valueOf(v.toString());
    }

    private List<String> asStringList(Object v) {
        if (v == null) return Collections.emptyList();
        if (v instanceof Array array) {
            try {
                Object[] arr = (Object[]) array.getArray();
                return Arrays.stream(arr).map(Object::toString).toList();
            } catch (SQLException e) {
                return Collections.emptyList();
            }
        }
        if (v instanceof Object[] arr) {
            return Arrays.stream(arr).map(Object::toString).toList();
        }
        return Collections.emptyList();
    }
}

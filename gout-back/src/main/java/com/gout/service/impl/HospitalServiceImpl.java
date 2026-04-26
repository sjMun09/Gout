package com.gout.service.impl;

import com.gout.client.KakaoLocalClient;
import com.gout.config.properties.KakaoProperties;
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
import com.gout.global.page.PageablePolicy;
import com.gout.service.HospitalService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Array;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional
public class HospitalServiceImpl implements HospitalService {

    private static final Pattern NORMALIZE_PATTERN = Pattern.compile("[^\\p{IsAlphabetic}\\p{IsDigit}]");

    private final HospitalRepository hospitalRepository;
    private final HospitalReviewRepository hospitalReviewRepository;
    private final KakaoLocalClient kakaoLocalClient;
    private final KakaoProperties kakaoProperties;

    @Override
    @Transactional(readOnly = true)
    public Page<HospitalResponse> search(HospitalSearchRequest request) {
        // #74: PageablePolicy.HOSPITAL 로 상한 도입. 정책이 page/size 를 동시에 보정한다.
        int safePage = PageablePolicy.HOSPITAL.clampPage(request.getPage());
        int safeSize = PageablePolicy.HOSPITAL.clampSize(request.getSize());
        Pageable pageable = PageablePolicy.HOSPITAL.toPageable(request.getPage(), request.getSize());
        // PG JDBC 는 JPQL 의 :keyword IS NULL 비교를 bytea 로 추론해 500 을 낸다.
        // 드라이버 버그를 우회하기 위해 null → "" 로 coalesce 하고 쿼리에서 LENGTH=0 으로 분기.
        String keyword = request.getKeyword() == null ? "" : request.getKeyword();

        if (request.hasLocation()) {
            // 네이티브 쿼리는 Pageable 미사용 → clamp 된 size/offset 을 직접 전달.
            int offset = safePage * safeSize;
            List<Object[]> rows = hospitalRepository.searchByLocation(
                    request.getLat(),
                    request.getLng(),
                    request.getRadius(),
                    keyword.isEmpty() ? null : keyword,
                    safeSize,
                    offset
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
            List<HospitalResponse> merged = mergeWithKakaoResults(
                    content,
                    request.getLat(),
                    request.getLng(),
                    request.getRadius(),
                    keyword,
                    safeSize
            );
            long mergedTotal = total + merged.stream()
                    .filter(hospital -> "KAKAO".equals(hospital.getSource()))
                    .count();
            return new PageImpl<>(merged, pageable, mergedTotal);
        }

        Pageable pageableWithSort = PageablePolicy.HOSPITAL.toPageable(
                request.getPage(), request.getSize(),
                Sort.by(Sort.Direction.ASC, "name"));
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
                .status(HospitalReview.Status.VISIBLE)
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
        // #74: 기존엔 size 상한이 없었다. PageablePolicy.HOSPITAL 로 상한 도입.
        Pageable pageable = PageablePolicy.HOSPITAL.toPageable(
                page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return hospitalReviewRepository.findByHospitalIdAndStatus(hospitalId, HospitalReview.Status.VISIBLE, pageable)
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

    private List<HospitalResponse> mergeWithKakaoResults(List<HospitalResponse> dbResults,
                                                         double lat,
                                                         double lng,
                                                         int radius,
                                                         String keyword,
                                                         int safeSize) {
        Map<String, HospitalResponse> merged = new LinkedHashMap<>();
        dbResults.forEach(hospital -> merged.put(deduplicationKey(hospital), hospital));

        List<KakaoLocalClient.KakaoHospitalPlace> kakaoPlaces = kakaoQueries(keyword).stream()
                .flatMap(query -> kakaoLocalClient.searchHospitals(lat, lng, radius, query, safeSize).stream())
                .toList();

        for (KakaoLocalClient.KakaoHospitalPlace place : kakaoPlaces) {
            HospitalResponse response = mapKakaoPlace(place);
            if (response == null) {
                continue;
            }
            merged.putIfAbsent(deduplicationKey(response), response);
        }

        return merged.values().stream()
                .sorted(Comparator.comparing(
                        HospitalResponse::getDistanceMeters,
                        Comparator.nullsLast(Double::compareTo)))
                .limit(safeSize)
                .toList();
    }

    private List<String> kakaoQueries(String keyword) {
        if (keyword != null && !keyword.isBlank()) {
            return List.of(keyword.trim());
        }
        return kakaoProperties.hospitalKeywordsOrDefault();
    }

    private HospitalResponse mapKakaoPlace(KakaoLocalClient.KakaoHospitalPlace place) {
        if (place.id() == null || place.id().isBlank() || place.place_name() == null || place.place_name().isBlank()) {
            return null;
        }
        String department = extractDepartment(place.category_name());
        return HospitalResponse.fromKakao(
                place.id(),
                place.place_name(),
                place.displayAddress(),
                place.phone(),
                asDouble(place.y()),
                asDouble(place.x()),
                department == null ? Collections.emptyList() : List.of(department),
                place.place_url(),
                asDouble(place.distance())
        );
    }

    private String extractDepartment(String categoryName) {
        if (categoryName == null || categoryName.isBlank()) {
            return null;
        }
        String[] parts = categoryName.split(">");
        String last = parts[parts.length - 1].trim();
        return last.isBlank() ? null : last;
    }

    private String deduplicationKey(HospitalResponse hospital) {
        String name = normalize(hospital.getName());
        String address = normalize(hospital.getAddress());
        if (address.isBlank()) {
            String lat = hospital.getLatitude() == null ? "" : String.format(Locale.ROOT, "%.5f", hospital.getLatitude());
            String lng = hospital.getLongitude() == null ? "" : String.format(Locale.ROOT, "%.5f", hospital.getLongitude());
            address = lat + "," + lng;
        }
        return name + "|" + address;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return NORMALIZE_PATTERN.matcher(value.toLowerCase(Locale.ROOT)).replaceAll("");
    }

    private String asString(Object v) {
        return v == null ? null : v.toString();
    }

    private Double asDouble(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.doubleValue();
        String value = v.toString();
        if (value.isBlank()) return null;
        return Double.valueOf(value);
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

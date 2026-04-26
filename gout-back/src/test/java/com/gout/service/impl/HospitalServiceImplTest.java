package com.gout.service.impl;

import com.gout.client.KakaoLocalClient;
import com.gout.config.properties.KakaoProperties;
import com.gout.dao.HospitalRepository;
import com.gout.dao.HospitalReviewRepository;
import com.gout.dto.request.HospitalSearchRequest;
import com.gout.dto.response.HospitalResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HospitalServiceImplTest {

    private final HospitalRepository hospitalRepository = mock(HospitalRepository.class);
    private final HospitalReviewRepository hospitalReviewRepository = mock(HospitalReviewRepository.class);
    private final KakaoLocalClient kakaoLocalClient = mock(KakaoLocalClient.class);
    private final HospitalServiceImpl hospitalService = new HospitalServiceImpl(
            hospitalRepository,
            hospitalReviewRepository,
            kakaoLocalClient,
            new KakaoProperties("rest-key", List.of("류마티스내과", "통풍"))
    );

    @Test
    @DisplayName("위치 검색은 DB 결과와 Kakao 결과를 병합하고 이름+주소 중복을 제거한다")
    void locationSearchMergesKakaoResultsAndDeduplicates() {
        HospitalSearchRequest request = new HospitalSearchRequest();
        request.setLat(37.5);
        request.setLng(127.0);
        request.setRadius(5_000);
        request.setSize(10);

        when(hospitalRepository.searchByLocation(37.5, 127.0, 5_000, null, 10, 0))
                .thenReturn(List.<Object[]>of(dbRow("db-1", "서울통풍내과", "서울 강남구 테헤란로 1", 100.0)));
        when(hospitalRepository.countByLocation(37.5, 127.0, 5_000, null))
                .thenReturn(1L);
        when(kakaoLocalClient.searchHospitals(anyDouble(), anyDouble(), anyInt(), eq("류마티스내과"), anyInt()))
                .thenReturn(List.of(
                        kakaoPlace("111", "서울 통풍 내과", "서울 강남구 테헤란로 1", "120"),
                        kakaoPlace("222", "강남류마티스의원", "서울 강남구 도산대로 2", "90")
                ));
        when(kakaoLocalClient.searchHospitals(anyDouble(), anyDouble(), anyInt(), eq("통풍"), anyInt()))
                .thenReturn(List.of(kakaoPlace("333", "강남통풍클리닉", "서울 강남구 봉은사로 3", "80")));

        Page<HospitalResponse> result = hospitalService.search(request);

        assertThat(result.getContent()).extracting(HospitalResponse::getId)
                .containsExactly("kakao:333", "kakao:222", "db-1");
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent()).filteredOn(hospital -> "KAKAO".equals(hospital.getSource()))
                .hasSize(2);
    }

    @Test
    @DisplayName("키워드가 있으면 Kakao 정책 키워드 대신 사용자 키워드만 사용한다")
    void locationSearchUsesUserKeywordForKakao() {
        HospitalSearchRequest request = new HospitalSearchRequest();
        request.setLat(37.5);
        request.setLng(127.0);
        request.setKeyword("요산");

        when(hospitalRepository.searchByLocation(37.5, 127.0, 5_000, "요산", 20, 0))
                .thenReturn(List.of());
        when(hospitalRepository.countByLocation(37.5, 127.0, 5_000, "요산"))
                .thenReturn(0L);
        when(kakaoLocalClient.searchHospitals(anyDouble(), anyDouble(), anyInt(), eq("요산"), anyInt()))
                .thenReturn(List.of());

        hospitalService.search(request);

        verify(kakaoLocalClient).searchHospitals(37.5, 127.0, 5_000, "요산", 20);
    }

    private Object[] dbRow(String id, String name, String address, Double distanceMeters) {
        return new Object[]{
                id,
                "HIRA",
                name,
                address,
                "02-111-1111",
                true,
                37.5,
                127.0,
                new String[]{"내과"},
                "09:00-18:00",
                distanceMeters
        };
    }

    private KakaoLocalClient.KakaoHospitalPlace kakaoPlace(String id,
                                                          String name,
                                                          String roadAddress,
                                                          String distance) {
        return new KakaoLocalClient.KakaoHospitalPlace(
                id,
                name,
                "의료,건강 > 병원 > 내과",
                "HP8",
                "병원",
                "02-222-2222",
                roadAddress,
                roadAddress,
                "127.0",
                "37.5",
                "https://place.map.kakao.com/" + id,
                distance
        );
    }
}

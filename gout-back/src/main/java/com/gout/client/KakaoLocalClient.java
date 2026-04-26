package com.gout.client;

import com.gout.config.properties.KakaoProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoLocalClient {

    private static final String KEYWORD_SEARCH_URL = "https://dapi.kakao.com/v2/local/search/keyword.json";
    private static final String HOSPITAL_CATEGORY_GROUP_CODE = "HP8";
    private static final int KAKAO_MAX_RADIUS_METERS = 20_000;
    private static final int KAKAO_MAX_SIZE = 15;

    private final RestTemplate restTemplate;
    private final KakaoProperties kakaoProperties;

    public List<KakaoHospitalPlace> searchHospitals(double lat, double lng, int radiusMeters, String query, int size) {
        String restApiKey = kakaoProperties.restApiKey();
        if (restApiKey == null || restApiKey.isBlank()) {
            return Collections.emptyList();
        }
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        URI uri = UriComponentsBuilder.fromUriString(KEYWORD_SEARCH_URL)
                .queryParam("query", query.trim())
                .queryParam("category_group_code", HOSPITAL_CATEGORY_GROUP_CODE)
                .queryParam("x", lng)
                .queryParam("y", lat)
                .queryParam("radius", Math.min(Math.max(radiusMeters, 0), KAKAO_MAX_RADIUS_METERS))
                .queryParam("page", 1)
                .queryParam("size", Math.min(Math.max(size, 1), KAKAO_MAX_SIZE))
                .queryParam("sort", "distance")
                .build()
                .encode()
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "KakaoAK " + restApiKey);

        try {
            ResponseEntity<KakaoLocalSearchResponse> response = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    KakaoLocalSearchResponse.class
            );
            KakaoLocalSearchResponse body = response.getBody();
            if (!response.getStatusCode().is2xxSuccessful() || body == null || body.documents() == null) {
                return Collections.emptyList();
            }
            return body.documents();
        } catch (RestClientException e) {
            log.warn("Kakao Local hospital search failed: {}", e.getClass().getSimpleName());
            return Collections.emptyList();
        }
    }

    public record KakaoLocalSearchResponse(
            List<KakaoHospitalPlace> documents
    ) {
    }

    public record KakaoHospitalPlace(
            String id,
            String place_name,
            String category_name,
            String category_group_code,
            String category_group_name,
            String phone,
            String address_name,
            String road_address_name,
            String x,
            String y,
            String place_url,
            String distance
    ) {
        public String displayAddress() {
            if (road_address_name != null && !road_address_name.isBlank()) {
                return road_address_name;
            }
            return address_name;
        }
    }
}

package com.gout.client;

import com.gout.config.properties.KakaoProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class KakaoLocalClientTest {

    @Test
    @DisplayName("REST 키가 없으면 Kakao Local API 를 호출하지 않고 빈 결과를 반환한다")
    void missingKeyReturnsEmpty() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        KakaoLocalClient client = new KakaoLocalClient(restTemplate, new KakaoProperties("", List.of("통풍")));

        assertThat(client.searchHospitals(37.5, 127.0, 5_000, "통풍", 20)).isEmpty();

        server.verify();
    }

    @Test
    @DisplayName("병원 카테고리와 거리 정렬로 Kakao Local keyword 검색을 호출한다")
    void callsKakaoKeywordSearch() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        KakaoLocalClient client = new KakaoLocalClient(restTemplate, new KakaoProperties("rest-key", List.of("통풍")));

        server.expect(once(), request -> {
                    assertThat(request.getURI().getHost()).isEqualTo("dapi.kakao.com");
                    assertThat(request.getURI().getPath()).isEqualTo("/v2/local/search/keyword.json");
                    assertThat(request.getURI().getRawQuery()).contains(
                            "query=%ED%86%B5%ED%92%8D",
                            "category_group_code=HP8",
                            "x=127.0",
                            "y=37.5",
                            "radius=20000",
                            "size=15",
                            "sort=distance"
                    );
                })
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "KakaoAK rest-key"))
                .andRespond(withSuccess("""
                        {
                          "documents": [
                            {
                              "id": "123",
                              "place_name": "서울통풍내과",
                              "category_name": "의료,건강 > 병원 > 내과",
                              "phone": "02-000-0000",
                              "address_name": "서울 강남구 역삼동 1",
                              "road_address_name": "서울 강남구 테헤란로 1",
                              "x": "127.0",
                              "y": "37.5",
                              "place_url": "https://place.map.kakao.com/123",
                              "distance": "120"
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        List<KakaoLocalClient.KakaoHospitalPlace> places =
                client.searchHospitals(37.5, 127.0, 30_000, "통풍", 20);

        assertThat(places).hasSize(1);
        assertThat(places.getFirst().id()).isEqualTo("123");
        assertThat(places.getFirst().displayAddress()).isEqualTo("서울 강남구 테헤란로 1");
        server.verify();
    }

    @Test
    @DisplayName("Kakao API 실패는 예외를 전파하지 않고 빈 결과로 폴백한다")
    void failureReturnsEmpty() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        KakaoLocalClient client = new KakaoLocalClient(restTemplate, new KakaoProperties("rest-key", List.of("통풍")));

        server.expect(once(), request -> assertThat(request.getURI().getHost()).isEqualTo("dapi.kakao.com"))
                .andRespond(withServerError());

        assertThat(client.searchHospitals(37.5, 127.0, 5_000, "통풍", 10)).isEmpty();
        server.verify();
    }
}

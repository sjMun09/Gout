package com.gout.config;

import com.gout.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P1-5: CORS 허용 오리진 화이트리스트 통합 테스트.
 *
 * <p>application-test.yml 의 {@code app.cors.allowed-origins} 화이트리스트
 * (http://localhost:3000, http://localhost:3010) 기준으로 preflight 를 검증.
 *
 * <p>{@link IntegrationTestBase} 는 gout-test-postgis-pgvector:local 이미지를 사용하는
 * Testcontainers 컨텍스트를 공유한다. 이미지 빌드 전제는 IntegrationTestBase Javadoc 참고.
 * (이미지 없음 시 @WebMvcTest 나 standalone MockMvc 로 대체 가능하나, 현재 로컬/CI 에 이미지 존재)
 */
class CorsConfigIntegrationTest extends IntegrationTestBase {

    private static final String ALLOWED_ORIGIN = "http://localhost:3000";
    private static final String DISALLOWED_ORIGIN = "https://evil.example.com";
    private static final String PREFLIGHT_PATH = "/api/auth/login";

    @Test
    @DisplayName("허용 오리진에서 preflight → 200 + Access-Control-Allow-Origin 매칭")
    void preflight_from_allowed_origin_returns_matching_allow_origin_header() throws Exception {
        // given
        String origin = ALLOWED_ORIGIN;

        // when
        var result = mockMvc.perform(options(PREFLIGHT_PATH)
                        .header(HttpHeaders.ORIGIN, origin)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type,Authorization"));

        // then
        result.andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin));
    }

    @Test
    @DisplayName("허용 오리진 preflight 응답에 Access-Control-Allow-Credentials: true 포함")
    void preflight_from_allowed_origin_includes_allow_credentials_header() throws Exception {
        // given
        String origin = ALLOWED_ORIGIN;

        // when
        var result = mockMvc.perform(options(PREFLIGHT_PATH)
                        .header(HttpHeaders.ORIGIN, origin)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"));

        // then
        result.andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    @DisplayName("차단된 오리진 preflight → Access-Control-Allow-Origin 없음 (CORS 거부)")
    void preflight_from_disallowed_origin_has_no_allow_origin_header() throws Exception {
        // given
        String origin = DISALLOWED_ORIGIN;

        // when
        var result = mockMvc.perform(options(PREFLIGHT_PATH)
                        .header(HttpHeaders.ORIGIN, origin)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"));

        // then
        // Spring Security CorsFilter 는 미허용 오리진에 대해 Access-Control-Allow-Origin 헤더를
        // 붙이지 않고 403 을 돌려준다 → 브라우저는 CORS 정책 위반으로 요청을 차단한다.
        result.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, nullValue()));
    }

    @Test
    @DisplayName("또 다른 허용 오리진(http://localhost:3010) 도 preflight 통과")
    void preflight_from_second_allowed_origin_is_accepted() throws Exception {
        // given
        String origin = "http://localhost:3010";

        // when
        var result = mockMvc.perform(options(PREFLIGHT_PATH)
                        .header(HttpHeaders.ORIGIN, origin)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"));

        // then
        result.andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }
}

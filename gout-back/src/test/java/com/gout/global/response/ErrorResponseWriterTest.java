package com.gout.global.response;

import com.gout.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ErrorResponseWriter} 단위 테스트 (Issue #85).
 *
 * <p>필터/시큐리티 핸들러가 만들어내는 에러 응답 shape 가
 * {@link com.gout.global.exception.GlobalExceptionHandler} 가 만드는 것과 동일하게
 * 직렬화되는지 검증한다. 회귀 핵심 — 프론트는 success/code/message/status/path 키만 보고 동작.
 */
class ErrorResponseWriterTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().build();
    private final ErrorResponseWriter writer = new ErrorResponseWriter(objectMapper);

    private MockHttpServletRequest req(String uri) {
        MockHttpServletRequest r = new MockHttpServletRequest();
        r.setRequestURI(uri);
        return r;
    }

    @Test
    void write_401_with_default_message_when_override_is_null() throws Exception {
        MockHttpServletRequest req = req("/api/me");
        MockHttpServletResponse resp = new MockHttpServletResponse();

        writer.write(req, resp, ErrorCode.UNAUTHORIZED, null);

        assertThat(resp.getStatus()).isEqualTo(401);
        // setContentType + setCharacterEncoding 이 둘 다 호출되면 MockHttpServletResponse 는
        // "application/json;charset=UTF-8" 형태로 합쳐 반환한다.
        assertThat(resp.getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
        assertThat(resp.getCharacterEncoding()).isEqualToIgnoringCase("UTF-8");
        assertThat(resp.getHeader(HttpHeaders.RETRY_AFTER)).isNull();

        JsonNode body = objectMapper.readTree(resp.getContentAsString());
        assertThat(body.path("success").asBoolean()).isFalse();
        assertThat(body.path("status").asInt()).isEqualTo(401);
        assertThat(body.path("code").asString()).isEqualTo("AUTH_UNAUTHORIZED");
        assertThat(body.path("message").asString()).isEqualTo(ErrorCode.UNAUTHORIZED.getMessage());
        assertThat(body.path("path").asString()).isEqualTo("/api/me");
        assertThat(Instant.parse(body.path("timestamp").asString())).isNotNull();
    }

    @Test
    void write_403_forbidden_with_default_message() throws Exception {
        MockHttpServletRequest req = req("/api/admin/reports");
        MockHttpServletResponse resp = new MockHttpServletResponse();

        writer.write(req, resp, ErrorCode.FORBIDDEN, null);

        assertThat(resp.getStatus()).isEqualTo(403);
        JsonNode body = objectMapper.readTree(resp.getContentAsString());
        assertThat(body.path("status").asInt()).isEqualTo(403);
        assertThat(body.path("code").asString()).isEqualTo("COMMON_FORBIDDEN");
        assertThat(body.path("message").asString()).isEqualTo(ErrorCode.FORBIDDEN.getMessage());
        assertThat(body.path("path").asString()).isEqualTo("/api/admin/reports");
    }

    @Test
    void write_uses_override_message_when_provided() throws Exception {
        MockHttpServletRequest req = req("/api/auth/login");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        String overrideMsg = "임의 사유로 막혔습니다.";

        writer.write(req, resp, ErrorCode.UNAUTHORIZED, overrideMsg);

        assertThat(resp.getStatus()).isEqualTo(401);
        JsonNode body = objectMapper.readTree(resp.getContentAsString());
        assertThat(body.path("code").asString()).isEqualTo("AUTH_UNAUTHORIZED");
        // override message 가 기본 message 를 대체해야 한다.
        assertThat(body.path("message").asString()).isEqualTo(overrideMsg);
        assertThat(body.path("message").asString()).isNotEqualTo(ErrorCode.UNAUTHORIZED.getMessage());
    }

    @Test
    void writeWithRetryAfter_sets_retry_after_header_and_429_status() throws Exception {
        MockHttpServletRequest req = req("/api/auth/login");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        String overrideMsg = "로그인 시도가 너무 많습니다. 잠시 후 다시 시도해주세요.";

        writer.writeWithRetryAfter(req, resp, ErrorCode.TOO_MANY_REQUESTS, overrideMsg, 60L);

        assertThat(resp.getStatus()).isEqualTo(429);
        assertThat(resp.getHeader(HttpHeaders.RETRY_AFTER)).isEqualTo("60");
        assertThat(resp.getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);

        JsonNode body = objectMapper.readTree(resp.getContentAsString());
        assertThat(body.path("status").asInt()).isEqualTo(429);
        assertThat(body.path("code").asString()).isEqualTo("COMMON_TOO_MANY_REQUESTS");
        assertThat(body.path("message").asString()).isEqualTo(overrideMsg);
        assertThat(body.path("path").asString()).isEqualTo("/api/auth/login");
    }

    @Test
    void write_handles_null_path_when_request_uri_missing() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        // setRequestURI 호출하지 않음 — MockHttpServletRequest 의 기본 RequestURI 는 ""
        // 명시적으로 null 인 경로를 재현하기 위해 setRequestURI(null) 호출.
        req.setRequestURI(null);
        MockHttpServletResponse resp = new MockHttpServletResponse();

        writer.write(req, resp, ErrorCode.UNAUTHORIZED, null);

        assertThat(resp.getStatus()).isEqualTo(401);
        JsonNode body = objectMapper.readTree(resp.getContentAsString());
        // ErrorResponse 는 @JsonInclude(NON_NULL) 이므로 path 가 null 이면 키 자체 누락.
        assertThat(body.has("path")).isFalse();
        assertThat(body.path("code").asString()).isEqualTo("AUTH_UNAUTHORIZED");
    }
}

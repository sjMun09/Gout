package com.gout.global.response;

import com.gout.global.exception.BusinessException;
import com.gout.global.exception.ErrorCode;
import com.gout.global.exception.GlobalExceptionHandler;
import com.gout.security.JwtAuthenticationFilter;
import com.gout.security.RateLimitExceededException;
import com.gout.security.RestAccessDeniedHandler;
import com.gout.security.RestAuthenticationEntryPoint;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.lang.reflect.Method;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 표준 ErrorResponse 응답 shape 가 모든 401/403/404/409/422/429/500 경로에서
 * 동일하게 직렬화되는지 검증하는 단위 테스트 (Issue #83).
 *
 * <p>통합 테스트(Testcontainers) 대신 핸들러를 직접 호출해 ResponseEntity / 직접 쓴 JSON 본문을
 * 검사한다. 이렇게 해도 모든 핸들러 경로 + JSON shape 가 동일하게 검증되며, CI 비용도 0 에 가깝다.
 */
class ErrorResponseSchemaTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().build();
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private MockHttpServletRequest req(String uri) {
        MockHttpServletRequest r = new MockHttpServletRequest();
        r.setRequestURI(uri);
        return r;
    }

    private void assertSchema(JsonNode body, int status, String code, String path) {
        assertThat(body.path("success").asBoolean()).isFalse();
        assertThat(body.path("code").asString()).isEqualTo(code);
        assertThat(body.path("status").asInt()).isEqualTo(status);
        assertThat(body.path("path").asString()).isEqualTo(path);
        assertThat(body.path("message").asString()).isNotBlank();
        // timestamp 는 ISO-8601 (Instant) 문자열로 직렬화되어야 한다.
        assertThat(body.path("timestamp").asString()).isNotEmpty();
        assertThat(Instant.parse(body.path("timestamp").asString())).isNotNull();
    }

    @Test
    void status_401_unauthorized_via_entry_point_for_expired_token() throws Exception {
        RestAuthenticationEntryPoint ep = new RestAuthenticationEntryPoint(objectMapper);
        MockHttpServletRequest req = req("/api/notifications");
        req.setAttribute(JwtAuthenticationFilter.ATTR_AUTH_ERROR, JwtAuthenticationFilter.ERROR_EXPIRED);
        MockHttpServletResponse resp = new MockHttpServletResponse();

        ep.commence(req, resp, new BadCredentialsException("expired"));

        assertThat(resp.getStatus()).isEqualTo(401);
        JsonNode body = objectMapper.readTree(resp.getContentAsString());
        assertSchema(body, 401, "EXPIRED_TOKEN", "/api/notifications");
    }

    @Test
    void status_401_unauthorized_via_entry_point_for_invalid_token() throws Exception {
        RestAuthenticationEntryPoint ep = new RestAuthenticationEntryPoint(objectMapper);
        MockHttpServletRequest req = req("/api/me");
        req.setAttribute(JwtAuthenticationFilter.ATTR_AUTH_ERROR, JwtAuthenticationFilter.ERROR_INVALID);
        MockHttpServletResponse resp = new MockHttpServletResponse();

        ep.commence(req, resp, new BadCredentialsException("invalid"));

        assertThat(resp.getStatus()).isEqualTo(401);
        JsonNode body = objectMapper.readTree(resp.getContentAsString());
        assertSchema(body, 401, "INVALID_TOKEN", "/api/me");
    }

    @Test
    void status_403_forbidden_via_access_denied_handler() throws Exception {
        RestAccessDeniedHandler h = new RestAccessDeniedHandler(objectMapper);
        MockHttpServletRequest req = req("/api/admin/reports");
        MockHttpServletResponse resp = new MockHttpServletResponse();

        h.handle(req, resp, new AccessDeniedException("denied"));

        assertThat(resp.getStatus()).isEqualTo(403);
        JsonNode body = objectMapper.readTree(resp.getContentAsString());
        assertSchema(body, 403, "FORBIDDEN", "/api/admin/reports");
    }

    @Test
    void status_404_not_found_via_business_exception() {
        ResponseEntity<ErrorResponse> resp = handler.handleBusinessException(
                new BusinessException(ErrorCode.USER_NOT_FOUND),
                req("/api/users/missing"));

        assertThat(resp.getStatusCode().value()).isEqualTo(404);
        ErrorResponse body = resp.getBody();
        assertThat(body).isNotNull();
        assertThat(body.success()).isFalse();
        assertThat(body.code()).isEqualTo("USER_NOT_FOUND");
        assertThat(body.status()).isEqualTo(404);
        assertThat(body.path()).isEqualTo("/api/users/missing");
        assertThat(body.message()).isEqualTo(ErrorCode.USER_NOT_FOUND.getMessage());
        assertThat(body.fieldErrors()).isNull();
    }

    @Test
    void status_409_conflict_via_business_exception() {
        ResponseEntity<ErrorResponse> resp = handler.handleBusinessException(
                new BusinessException(ErrorCode.DUPLICATE_REPORT),
                req("/api/reports"));

        assertThat(resp.getStatusCode().value()).isEqualTo(409);
        ErrorResponse body = resp.getBody();
        assertThat(body).isNotNull();
        assertThat(body.code()).isEqualTo("DUPLICATE_REPORT");
        assertThat(body.status()).isEqualTo(409);
        assertThat(body.path()).isEqualTo("/api/reports");
    }

    @Test
    void status_422_validation_with_field_errors() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "registerRequest");
        bindingResult.addError(new FieldError("registerRequest", "email", "", false,
                new String[]{"NotBlank"}, null, "이메일은 필수입니다."));
        bindingResult.addError(new FieldError("registerRequest", "password", "x", false,
                new String[]{"Size"}, null, "비밀번호는 8자 이상이어야 합니다."));

        Method m = this.getClass().getDeclaredMethod("dummyForBinding", String.class);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(
                new org.springframework.core.MethodParameter(m, 0), bindingResult);

        ResponseEntity<ErrorResponse> resp = handler.handleValidationException(ex, req("/api/auth/register"));

        assertThat(resp.getStatusCode().value()).isEqualTo(422);
        ErrorResponse body = resp.getBody();
        assertThat(body).isNotNull();
        assertThat(body.code()).isEqualTo("INVALID_INPUT");
        assertThat(body.status()).isEqualTo(422);
        assertThat(body.path()).isEqualTo("/api/auth/register");
        assertThat(body.fieldErrors()).hasSize(2);
        ErrorResponse.FieldErrorDetail first = body.fieldErrors().get(0);
        assertThat(first.field()).isEqualTo("email");
        assertThat(first.code()).isEqualTo("NotBlank");
        assertThat(first.message()).isEqualTo("이메일은 필수입니다.");
    }

    @Test
    void status_429_too_many_requests_via_rate_limit_exception() {
        ResponseEntity<ErrorResponse> resp = handler.handleRateLimitExceeded(
                new RateLimitExceededException("로그인 시도가 너무 많습니다.", 60L),
                req("/api/auth/login"));

        assertThat(resp.getStatusCode().value()).isEqualTo(429);
        assertThat(resp.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("60");
        ErrorResponse body = resp.getBody();
        assertThat(body).isNotNull();
        assertThat(body.code()).isEqualTo("TOO_MANY_REQUESTS");
        assertThat(body.status()).isEqualTo(429);
        assertThat(body.path()).isEqualTo("/api/auth/login");
        assertThat(body.message()).isEqualTo("로그인 시도가 너무 많습니다.");
    }

    @Test
    void status_500_internal_server_error_for_unexpected_exception() {
        ResponseEntity<ErrorResponse> resp = handler.handleException(
                new RuntimeException("kaboom"),
                req("/api/posts/abc"));

        assertThat(resp.getStatusCode().value()).isEqualTo(500);
        ErrorResponse body = resp.getBody();
        assertThat(body).isNotNull();
        assertThat(body.code()).isEqualTo("INTERNAL_SERVER_ERROR");
        assertThat(body.status()).isEqualTo(500);
        assertThat(body.path()).isEqualTo("/api/posts/abc");
        // 내부 메시지(kaboom) 가 노출되지 않고 표준 메시지로 마스킹되는지 확인.
        assertThat(body.message()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR.getMessage());
    }

    @Test
    void status_404_not_found_via_no_resource_found() {
        ResponseEntity<ErrorResponse> resp = handler.handleNoResourceFound(
                new NoResourceFoundException(org.springframework.http.HttpMethod.GET, "/static/missing.png", "static"),
                req("/static/missing.png"));

        assertThat(resp.getStatusCode().value()).isEqualTo(404);
        ErrorResponse body = resp.getBody();
        assertThat(body).isNotNull();
        assertThat(body.code()).isEqualTo("NOT_FOUND");
        assertThat(body.status()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(body.path()).isEqualTo("/static/missing.png");
    }

    @SuppressWarnings("unused")
    private void dummyForBinding(String body) {}

    @Test
    void api_response_backwards_compat_keys_present() throws Exception {
        // 기존 프론트가 success/message/data 만 보고 동작하므로, 새 ErrorResponse 도 그대로 노출되어야 한다.
        ErrorResponse body = ErrorResponse.of(ErrorCode.POST_NOT_FOUND,
                ErrorCode.POST_NOT_FOUND.getMessage(), "/api/posts/x");
        String json = objectMapper.writeValueAsString(body);
        JsonNode node = objectMapper.readTree(json);

        assertThat(node.has("success")).isTrue();
        assertThat(node.has("message")).isTrue();
        // data 는 NON_NULL 정책이라 null 일 때 키 자체가 빠진다 — 의도된 동작.
        assertThat(node.has("data")).isFalse();
    }

    // path 인자가 null 인 케이스 — Security 핸들러에서 RequestURI 가 비는 경우 대비.
    @Test
    void error_response_handles_null_path() throws Exception {
        ErrorResponse body = ErrorResponse.of(ErrorCode.UNAUTHORIZED, "x", null);
        String json = objectMapper.writeValueAsString(body);
        JsonNode node = objectMapper.readTree(json);
        // NON_NULL 정책 — path 가 null 이면 키 자체 누락.
        assertThat(node.has("path")).isFalse();
    }

    // 인증 실패 직접 호출 (필터 외 경로에서 발생) — handler.handleAuthenticationException
    @Test
    void status_401_via_authentication_exception_handler() {
        ResponseEntity<ErrorResponse> resp = handler.handleAuthenticationException(
                new BadCredentialsException("bad"),
                req("/api/me"));

        assertThat(resp.getStatusCode().value()).isEqualTo(401);
        ErrorResponse body = resp.getBody();
        assertThat(body).isNotNull();
        assertThat(body.code()).isEqualTo("UNAUTHORIZED");
        assertThat(body.path()).isEqualTo("/api/me");
    }

}

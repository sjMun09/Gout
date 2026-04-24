package com.gout.security;

import com.gout.global.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 미인증 요청(= 인증이 필요하나 SecurityContext 비어있음)에 대한 401 응답을 표준화한다.
 *
 * <p>JwtAuthenticationFilter 가 {@code request.attr[auth.error]} 로 남긴 사유를 읽어
 * {@code token_expired} / {@code invalid_token} / {@code unauthorized} 로 code 를 분기한다.
 *
 * <p>응답 포맷은 기존 {@link ApiResponse} 와 호환하면서 RFC 7807 스타일 code 필드를 추가.
 * <pre>
 * {
 *   "success": false,
 *   "code": "token_expired",
 *   "message": "만료된 토큰입니다.",
 *   "data": null
 * }
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        String code = (String) request.getAttribute(JwtAuthenticationFilter.ATTR_AUTH_ERROR);
        String message;
        if (JwtAuthenticationFilter.ERROR_EXPIRED.equals(code)) {
            message = "만료된 토큰입니다.";
        } else if (JwtAuthenticationFilter.ERROR_INVALID.equals(code)) {
            message = "유효하지 않은 토큰입니다.";
        } else {
            code = "unauthorized";
            message = "로그인이 필요합니다.";
        }

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("code", code);
        body.put("message", message);
        body.put("data", null);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}

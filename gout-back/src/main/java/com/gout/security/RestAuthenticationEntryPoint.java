package com.gout.security;

import com.gout.global.exception.ErrorCode;
import com.gout.global.response.ErrorResponse;
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

/**
 * 미인증 요청에 대한 401 응답을 표준화한다 ({@link ErrorResponse} shape 사용).
 *
 * <p>JwtAuthenticationFilter 가 {@code request.attr[auth.error]} 로 남긴 사유를 읽어
 * {@code TOKEN_EXPIRED} / {@code INVALID_TOKEN} / {@code UNAUTHORIZED} 로 code 를 분기.
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
        String reason = (String) request.getAttribute(JwtAuthenticationFilter.ATTR_AUTH_ERROR);
        String code;
        String message;
        if (JwtAuthenticationFilter.ERROR_EXPIRED.equals(reason)) {
            code = "TOKEN_EXPIRED";
            message = ErrorCode.EXPIRED_TOKEN.getMessage();
        } else if (JwtAuthenticationFilter.ERROR_INVALID.equals(reason)) {
            code = ErrorCode.INVALID_TOKEN.name();
            message = ErrorCode.INVALID_TOKEN.getMessage();
        } else {
            code = ErrorCode.UNAUTHORIZED.name();
            message = ErrorCode.UNAUTHORIZED.getMessage();
        }

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ErrorResponse body = ErrorResponse.of(
                HttpStatus.UNAUTHORIZED.value(),
                code,
                message,
                request.getRequestURI()
        );
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}

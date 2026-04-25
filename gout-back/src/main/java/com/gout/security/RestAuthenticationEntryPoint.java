package com.gout.security;

import com.gout.global.exception.ErrorCode;
import com.gout.global.response.ErrorResponse;
import com.gout.global.response.ErrorResponseWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 미인증 요청에 대한 401 응답을 표준화한다 ({@link ErrorResponse} shape 사용).
 *
 * <p>JwtAuthenticationFilter 가 {@code request.attr[auth.error]} 로 남긴 사유를 읽어
 * {@code AUTH_EXPIRED_TOKEN} / {@code AUTH_INVALID_TOKEN} / {@code AUTH_UNAUTHORIZED} 로 code 를 분기.
 *
 * <p>본문 직렬화는 {@link ErrorResponseWriter} 에 위임.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ErrorResponseWriter errorResponseWriter;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        String reason = (String) request.getAttribute(JwtAuthenticationFilter.ATTR_AUTH_ERROR);
        ErrorCode errorCode;
        if (JwtAuthenticationFilter.ERROR_EXPIRED.equals(reason)) {
            errorCode = ErrorCode.EXPIRED_TOKEN;
        } else if (JwtAuthenticationFilter.ERROR_INVALID.equals(reason)) {
            errorCode = ErrorCode.INVALID_TOKEN;
        } else {
            errorCode = ErrorCode.UNAUTHORIZED;
        }

        errorResponseWriter.write(request, response, errorCode, null);
    }
}

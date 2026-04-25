package com.gout.security;

import com.gout.global.exception.ErrorCode;
import com.gout.global.response.ErrorResponse;
import com.gout.global.response.ErrorResponseWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 인증은 됐지만 권한 부족 시 403 응답 포맷을 {@link ErrorResponse} 로 표준화.
 *
 * <p>본문 직렬화는 {@link ErrorResponseWriter} 에 위임 — 같은 핸들러 셋 (필터/엔트리포인트)
 * 이 동일 shape 를 갖도록 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ErrorResponseWriter errorResponseWriter;

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        errorResponseWriter.write(request, response, ErrorCode.FORBIDDEN, null);
    }
}

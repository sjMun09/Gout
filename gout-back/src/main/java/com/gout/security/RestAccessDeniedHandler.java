package com.gout.security;

import com.gout.global.exception.ErrorCode;
import com.gout.global.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * 인증은 됐지만 권한 부족 시 403 응답 포맷을 {@link ErrorResponse} 로 표준화.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ErrorResponse body = ErrorResponse.of(
                ErrorCode.FORBIDDEN,
                ErrorCode.FORBIDDEN.getMessage(),
                request.getRequestURI()
        );
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}

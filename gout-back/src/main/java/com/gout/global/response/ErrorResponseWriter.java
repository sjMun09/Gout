package com.gout.global.response;

import com.gout.global.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * 필터/시큐리티 핸들러가 컨트롤러 진입 전에 작성하는 에러 응답을
 * {@link ErrorResponse} 표준 shape 로 통일해서 직렬화하는 헬퍼 컴포넌트.
 *
 * <p>이전에는 {@link com.gout.security.RestAuthenticationEntryPoint},
 * {@link com.gout.security.RestAccessDeniedHandler},
 * {@link com.gout.security.RateLimitFilter} 가 각각 {@code ObjectMapper} 를 직접 잡고
 * status / Content-Type / encoding / 본문 직렬화를 인라인으로 작성하던 것을 한 곳으로 모은다.
 *
 * <p>회귀 무위험: 만들어내는 JSON shape 는 기존 셋과 동일하다(같은 {@link ErrorResponse#of}
 * factory + 같은 {@code ObjectMapper} 사용). 프론트의 {@code lib/api/handleApiError.ts}
 * / {@code client.ts} 가 보는 키(success/code/message/status/path/timestamp)는 그대로 유지된다.
 */
@Component
public class ErrorResponseWriter {

    private final ObjectMapper objectMapper;

    public ErrorResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 표준 에러 응답을 본문에 작성한다.
     *
     * @param request         servlet request — {@code getRequestURI()} 를 path 로 사용
     * @param response        servlet response — status/headers/body 를 채움
     * @param errorCode       표준 에러 코드 (status/code 의 source of truth)
     * @param overrideMessage null 이면 {@code errorCode.getMessage()} 를 그대로 사용,
     *                        아니면 override message 가 그대로 message 필드로 들어간다
     */
    public void write(HttpServletRequest request,
                      HttpServletResponse response,
                      ErrorCode errorCode,
                      String overrideMessage) throws IOException {
        writeInternal(request, response, errorCode, overrideMessage, null);
    }

    /**
     * 429 Too Many Requests 같은 응답에서 {@code Retry-After} 헤더를 같이 붙여야 할 때 사용한다.
     *
     * @param retryAfterSeconds {@code Retry-After} 헤더에 초 단위로 기록할 값
     */
    public void writeWithRetryAfter(HttpServletRequest request,
                                    HttpServletResponse response,
                                    ErrorCode errorCode,
                                    String overrideMessage,
                                    long retryAfterSeconds) throws IOException {
        writeInternal(request, response, errorCode, overrideMessage, retryAfterSeconds);
    }

    private void writeInternal(HttpServletRequest request,
                               HttpServletResponse response,
                               ErrorCode errorCode,
                               String overrideMessage,
                               Long retryAfterSeconds) throws IOException {
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        if (retryAfterSeconds != null) {
            response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
        }

        String message = (overrideMessage != null) ? overrideMessage : errorCode.getMessage();
        String path = (request != null) ? request.getRequestURI() : null;
        ErrorResponse body = ErrorResponse.of(errorCode, message, path);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}

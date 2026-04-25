package com.gout.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.gout.global.exception.ErrorCode;

import java.time.Instant;
import java.util.List;

/**
 * 표준 에러 응답 본문.
 *
 * <p>모든 4xx/5xx 응답이 동일한 JSON shape 를 갖도록 하기 위한 record.
 * GlobalExceptionHandler / RestAuthenticationEntryPoint / RestAccessDeniedHandler / RateLimitFilter
 * 가 공통으로 사용한다.
 *
 * <p>호환성: 기존 {@link ApiResponse} 가 가지던 success/message/data 키를 모두 유지한다.
 * code/status/path/timestamp/fieldErrors 는 추가 필드.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        boolean success,
        String code,
        String message,
        int status,
        String path,
        Instant timestamp,
        List<FieldErrorDetail> fieldErrors,
        Object data
) {

    public static ErrorResponse of(ErrorCode errorCode, String message, String path) {
        return new ErrorResponse(
                false,
                errorCode.getCode(),
                message,
                errorCode.getStatus().value(),
                path,
                Instant.now(),
                null,
                null
        );
    }

    public static ErrorResponse of(int status, String code, String message, String path) {
        return new ErrorResponse(
                false,
                code,
                message,
                status,
                path,
                Instant.now(),
                null,
                null
        );
    }

    public static ErrorResponse validation(String path, List<FieldErrorDetail> fieldErrors, String message) {
        return new ErrorResponse(
                false,
                ErrorCode.INVALID_INPUT.getCode(),
                message,
                422,
                path,
                Instant.now(),
                fieldErrors,
                null
        );
    }

    public record FieldErrorDetail(String field, String code, String message) {}
}

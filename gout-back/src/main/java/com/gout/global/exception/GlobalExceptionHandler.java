package com.gout.global.exception;

import com.gout.global.response.ErrorResponse;
import com.gout.security.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e, HttpServletRequest req) {
        log.warn("BusinessException: {}", e.getMessage());
        return ResponseEntity
                .status(e.getErrorCode().getStatus())
                .body(ErrorResponse.of(e.getErrorCode(), e.getMessage(), req.getRequestURI()));
    }

    /**
     * Bean Validation 실패 — 422 Unprocessable Entity 로 통일하고 fieldErrors 를 구조화한다.
     * 기존 400 에서 변경: validation 실패는 RFC 4918 의 422 가 더 정확.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e,
                                                                    HttpServletRequest req) {
        List<ErrorResponse.FieldErrorDetail> details = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorResponse.FieldErrorDetail(
                        fe.getField(),
                        fe.getCode(),
                        fe.getDefaultMessage()))
                .toList();
        String summary = details.stream()
                .map(ErrorResponse.FieldErrorDetail::message)
                .filter(m -> m != null && !m.isBlank())
                .findFirst()
                .orElse(ErrorCode.INVALID_INPUT.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ErrorResponse.validation(req.getRequestURI(), details, summary));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException e, HttpServletRequest req) {
        log.warn("AccessDenied: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(ErrorCode.FORBIDDEN, ErrorCode.FORBIDDEN.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException e,
                                                                       HttpServletRequest req) {
        log.warn("AuthenticationException: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of(ErrorCode.UNAUTHORIZED, ErrorCode.UNAUTHORIZED.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimitExceeded(RateLimitExceededException e,
                                                                 HttpServletRequest req) {
        log.warn("RateLimitExceeded: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(e.getRetryAfterSeconds()))
                .body(ErrorResponse.of(
                        HttpStatus.TOO_MANY_REQUESTS.value(),
                        "TOO_MANY_REQUESTS",
                        e.getMessage(),
                        req.getRequestURI()));
    }

    /**
     * 정적 리소스 매칭 실패 — Spring 의 NoResourceFoundException 은 catch-all Exception 으로 흘러
     * 500 으로 잘못 분류되던 문제 방지.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException e, HttpServletRequest req) {
        log.warn("NoResourceFound: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(ErrorCode.NOT_FOUND, ErrorCode.NOT_FOUND.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e, HttpServletRequest req) {
        log.error("Unexpected error", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(
                        ErrorCode.INTERNAL_SERVER_ERROR,
                        ErrorCode.INTERNAL_SERVER_ERROR.getMessage(),
                        req.getRequestURI()));
    }
}

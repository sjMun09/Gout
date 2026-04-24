package com.gout.controller;

import com.gout.global.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * @PreAuthorize 실패 시 AuthorizationDeniedException / AccessDeniedException 이
 * DispatcherServlet 까지 전파되는데, 기본 GlobalExceptionHandler 가 Exception.class 로
 * 모두 잡아서 500 을 반환하는 문제를 해결하기 위한 전용 어드바이스.
 *
 * @Order 로 우선순위를 올려 GlobalExceptionHandler 보다 먼저 매칭되도록 한다.
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class AdminExceptionHandler {

    @ExceptionHandler({ AuthorizationDeniedException.class, AccessDeniedException.class })
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(Exception e) {
        log.info("Access denied: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("접근 권한이 없습니다."));
    }
}

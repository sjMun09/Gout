package com.gout.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Auth
    DUPLICATE_EMAIL(HttpStatus.BAD_REQUEST, "AUTH_DUPLICATE_EMAIL", "이미 가입된 이메일입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH_INVALID_CREDENTIALS", "이메일 또는 비밀번호가 올바르지 않습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_INVALID_TOKEN", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_EXPIRED_TOKEN", "만료된 토큰입니다."),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."),

    // Hospital
    HOSPITAL_NOT_FOUND(HttpStatus.NOT_FOUND, "HOSPITAL_NOT_FOUND", "병원을 찾을 수 없습니다."),
    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "HOSPITAL_REVIEW_NOT_FOUND", "리뷰를 찾을 수 없습니다."),
    DUPLICATE_REVIEW(HttpStatus.BAD_REQUEST, "HOSPITAL_DUPLICATE_REVIEW", "이미 해당 날짜에 리뷰를 작성했습니다."),

    // Food
    FOOD_NOT_FOUND(HttpStatus.NOT_FOUND, "FOOD_NOT_FOUND", "음식 정보를 찾을 수 없습니다."),

    // Community
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMUNITY_POST_NOT_FOUND", "게시글을 찾을 수 없습니다."),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMUNITY_COMMENT_NOT_FOUND", "댓글을 찾을 수 없습니다."),
    DUPLICATE_LIKE(HttpStatus.BAD_REQUEST, "COMMUNITY_DUPLICATE_LIKE", "이미 좋아요를 눌렀습니다."),
    LIKE_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMUNITY_LIKE_NOT_FOUND", "좋아요를 찾을 수 없습니다."),
    DUPLICATE_REPORT(HttpStatus.CONFLICT, "COMMUNITY_DUPLICATE_REPORT", "이미 신고한 대상입니다."),
    REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMUNITY_REPORT_NOT_FOUND", "신고를 찾을 수 없습니다."),
    INVALID_REPORT_STATUS(HttpStatus.BAD_REQUEST, "COMMUNITY_INVALID_REPORT_STATUS", "유효하지 않은 신고 상태입니다."),

    // Health
    HEALTH_LOG_NOT_FOUND(HttpStatus.NOT_FOUND, "HEALTH_LOG_NOT_FOUND", "기록을 찾을 수 없습니다."),

    // Content
    CONTENT_NOT_FOUND(HttpStatus.NOT_FOUND, "CONTENT_NOT_FOUND", "콘텐츠를 찾을 수 없습니다."),
    AGE_CONTENT_NOT_FOUND(HttpStatus.NOT_FOUND, "AGE_CONTENT_NOT_FOUND", "연령별 콘텐츠를 찾을 수 없습니다."),
    PAPER_NOT_FOUND(HttpStatus.NOT_FOUND, "PAPER_NOT_FOUND", "논문 정보를 찾을 수 없습니다."),
    GUIDELINE_NOT_FOUND(HttpStatus.NOT_FOUND, "GUIDELINE_NOT_FOUND", "가이드라인을 찾을 수 없습니다."),

    // Common
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_UNAUTHORIZED", "로그인이 필요합니다."),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "COMMON_INVALID_INPUT", "잘못된 입력값입니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON_FORBIDDEN", "접근 권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON_NOT_FOUND", "요청한 리소스를 찾을 수 없습니다."),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "COMMON_TOO_MANY_REQUESTS", "요청이 너무 많습니다. 잠시 후 다시 시도해주세요."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}

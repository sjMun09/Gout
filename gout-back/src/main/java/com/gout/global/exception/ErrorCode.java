package com.gout.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Auth
    DUPLICATE_EMAIL(400, "이미 가입된 이메일입니다."),
    INVALID_CREDENTIALS(401, "이메일 또는 비밀번호가 올바르지 않습니다."),
    INVALID_TOKEN(401, "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(401, "만료된 토큰입니다."),

    // User
    USER_NOT_FOUND(404, "사용자를 찾을 수 없습니다."),

    // Hospital
    HOSPITAL_NOT_FOUND(404, "병원을 찾을 수 없습니다."),
    REVIEW_NOT_FOUND(404, "리뷰를 찾을 수 없습니다."),
    DUPLICATE_REVIEW(400, "이미 해당 날짜에 리뷰를 작성했습니다."),

    // Food
    FOOD_NOT_FOUND(404, "음식 정보를 찾을 수 없습니다."),

    // Community
    POST_NOT_FOUND(404, "게시글을 찾을 수 없습니다."),
    COMMENT_NOT_FOUND(404, "댓글을 찾을 수 없습니다."),
    DUPLICATE_LIKE(400, "이미 좋아요를 눌렀습니다."),
    LIKE_NOT_FOUND(404, "좋아요를 찾을 수 없습니다."),

    // Health
    HEALTH_LOG_NOT_FOUND(404, "기록을 찾을 수 없습니다."),

    // Content
    CONTENT_NOT_FOUND(404, "콘텐츠를 찾을 수 없습니다."),
    AGE_CONTENT_NOT_FOUND(404, "연령별 콘텐츠를 찾을 수 없습니다."),
    PAPER_NOT_FOUND(404, "논문 정보를 찾을 수 없습니다."),
    GUIDELINE_NOT_FOUND(404, "가이드라인을 찾을 수 없습니다."),

    // Common
    UNAUTHORIZED(401, "로그인이 필요합니다."),
    INVALID_INPUT(400, "잘못된 입력값입니다."),
    FORBIDDEN(403, "접근 권한이 없습니다."),
    NOT_FOUND(404, "요청한 리소스를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(500, "서버 오류가 발생했습니다.");

    private final int status;
    private final String message;
}

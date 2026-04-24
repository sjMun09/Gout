package com.gout.security;

/**
 * Bucket4j 토큰을 모두 소진했을 때 던지는 예외.
 * GlobalExceptionHandler 에서 429 Too Many Requests + Retry-After 헤더로 변환한다.
 */
public class RateLimitExceededException extends RuntimeException {

    private final long retryAfterSeconds;

    public RateLimitExceededException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}

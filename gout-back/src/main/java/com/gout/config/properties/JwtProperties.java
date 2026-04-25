package com.gout.config.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * jwt.* 설정 바인딩.
 *
 * <p>secret 은 HS256 키이므로 32바이트(256비트) 미달이면 기동 단계에서 {@link Validated} 검증으로 실패시킨다.
 * 문자열 길이 기준 검증이라 실제 바이트 길이와 1:1 일치하진 않지만 (멀티바이트 문자), 32 미만이면 무조건 256비트 미만이므로
 * 1차 방어선으로 충분. 정확한 byte 길이 가드는 {@code JwtTokenProvider} 생성자가 유지한다(2차 방어선).
 *
 * <p>access/refresh 만료는 long 밀리초 — 기존 application.yml 값(900000 / 604800000)과 호환되도록 raw long 유지.
 */
@ConfigurationProperties(prefix = "jwt")
@Validated
public record JwtProperties(
        @NotBlank @Size(min = 32) String secret,
        @Positive long accessTokenExpiry,
        @Positive long refreshTokenExpiry
) {
}

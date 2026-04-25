package com.gout.config.properties;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * app.cors.* 설정 바인딩.
 *
 * <p>allowedOrigins 는 비어 있으면 안 된다 — 운영에서 와일드카드 금지(allowCredentials 와 호환되지 않으므로).
 * dev 는 application.yml 기본값(localhost:3000,3010)을 가지고, prod 는 환경변수 미설정 시 placeholder
 * resolution 단계에서 실패한다.
 */
@ConfigurationProperties(prefix = "app.cors")
@Validated
public record CorsProperties(
        @NotEmpty List<String> allowedOrigins
) {
}

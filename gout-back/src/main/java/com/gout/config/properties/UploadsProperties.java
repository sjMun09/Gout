package com.gout.config.properties;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * app.uploads.* 설정 바인딩.
 *
 * <p>baseDir 미지정 시 기동 실패. dev 기본값({@code ./uploads}), prod 기본값({@code /app/uploads})은
 * application-{profile}.yml 에서 주입된다.
 */
@ConfigurationProperties(prefix = "app.uploads")
@Validated
public record UploadsProperties(
        @NotBlank String baseDir
) {
}

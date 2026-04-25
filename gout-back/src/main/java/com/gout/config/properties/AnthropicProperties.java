package com.gout.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * app.anthropic.* 설정 바인딩.
 *
 * <p>apiKey 는 빈 문자열 허용 — 미설정 시 PaperAiService 가 NOOP.
 */
@ConfigurationProperties(prefix = "app.anthropic")
public record AnthropicProperties(
        String apiKey
) {
}

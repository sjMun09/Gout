package com.gout.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * app.openai.* 설정 바인딩.
 *
 * <p>apiKey 는 빈 문자열 허용 — 미설정 시 PaperEmbeddingService 가 NOOP. 강제 검증은 의도적으로 두지 않는다
 * (크롤러 비활성 환경에서도 기동 가능해야 함).
 */
@ConfigurationProperties(prefix = "app.openai")
public record OpenAiProperties(
        String apiKey
) {
}

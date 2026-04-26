package com.gout.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * app.kakao.* 설정 바인딩.
 *
 * <p>restApiKey 는 서버 전용 Kakao REST API 키다. 브라우저로 내려보내지 않는다.
 */
@ConfigurationProperties(prefix = "app.kakao")
public record KakaoProperties(
        String restApiKey,
        List<String> hospitalKeywords
) {
    public List<String> hospitalKeywordsOrDefault() {
        if (hospitalKeywords == null || hospitalKeywords.isEmpty()) {
            return List.of("류마티스내과", "통풍", "내과");
        }
        List<String> normalized = hospitalKeywords.stream()
                .filter(keyword -> keyword != null && !keyword.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
        return normalized.isEmpty() ? List.of("류마티스내과", "통풍", "내과") : normalized;
    }
}

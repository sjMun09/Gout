package com.gout.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * app.crawler.* 설정 바인딩.
 */
@ConfigurationProperties(prefix = "app.crawler")
public record CrawlerProperties(
        boolean enabled
) {
}

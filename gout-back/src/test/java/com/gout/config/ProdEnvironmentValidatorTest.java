package com.gout.config;

import com.gout.config.properties.AnthropicProperties;
import com.gout.config.properties.CorsProperties;
import com.gout.config.properties.CrawlerProperties;
import com.gout.config.properties.JwtProperties;
import com.gout.config.properties.OpenAiProperties;
import com.gout.config.properties.UploadsProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProdEnvironmentValidatorTest {

    private static final String JWT_SECRET = "prod-secret-at-least-thirty-two-characters-long";

    @Test
    void validatePassesWhenProductionEnvIsCompleteAndCrawlerDisabled() {
        ProdEnvironmentValidator validator = validator(
                completeEnv().withProperty("spring.profiles.active", "prod"),
                new CrawlerProperties(false),
                new AnthropicProperties(""),
                new OpenAiProperties("")
        );

        assertThat(validator.validate()).isEmpty();
        assertThatCode(validator::afterPropertiesSet).doesNotThrowAnyException();
    }

    @Test
    void validateRequiresCrawlerKeysWhenCrawlerIsEnabled() {
        ProdEnvironmentValidator validator = validator(
                completeEnv().withProperty("spring.profiles.active", "prod"),
                new CrawlerProperties(true),
                new AnthropicProperties(""),
                new OpenAiProperties(" ")
        );

        assertThat(validator.validate()).containsExactly(
                "PAPER_CRAWLER_ENABLED=true requires ANTHROPIC_API_KEY for app.anthropic.api-key",
                "PAPER_CRAWLER_ENABLED=true requires OPENAI_API_KEY for app.openai.api-key"
        );
        assertThatThrownBy(validator::afterPropertiesSet)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Production environment validation failed")
                .hasMessageContaining("ANTHROPIC_API_KEY")
                .hasMessageContaining("OPENAI_API_KEY");
    }

    @Test
    void validateReportsMissingProductionInfrastructureEnv() {
        ProdEnvironmentValidator validator = validator(
                new MockEnvironment()
                        .withProperty("spring.profiles.active", "prod")
                        .withProperty("spring.data.redis.port", "not-a-port"),
                new CrawlerProperties(false),
                new AnthropicProperties(""),
                new OpenAiProperties("")
        );

        assertThat(validator.validate()).contains(
                "DB_URL must be set for spring.datasource.url",
                "DB_USERNAME must be set for spring.datasource.username",
                "DB_PASSWORD must be set for spring.datasource.password",
                "REDIS_HOST must be set for spring.data.redis.host",
                "REDIS_PORT must be a positive integer for spring.data.redis.port"
        );
    }

    @Test
    void afterPropertiesSetSkipsValidationOutsideProdProfile() {
        ProdEnvironmentValidator validator = validator(
                new MockEnvironment().withProperty("spring.profiles.active", "dev"),
                new CrawlerProperties(true),
                new AnthropicProperties(""),
                new OpenAiProperties("")
        );

        assertThatCode(validator::afterPropertiesSet).doesNotThrowAnyException();
    }

    private static MockEnvironment completeEnv() {
        return new MockEnvironment()
                .withProperty("spring.datasource.url", "jdbc:postgresql://db:5432/gout")
                .withProperty("spring.datasource.username", "gout")
                .withProperty("spring.datasource.password", "secret")
                .withProperty("spring.data.redis.host", "redis")
                .withProperty("spring.data.redis.port", "6379");
    }

    private static ProdEnvironmentValidator validator(
            MockEnvironment environment,
            CrawlerProperties crawlerProperties,
            AnthropicProperties anthropicProperties,
            OpenAiProperties openAiProperties
    ) {
        return new ProdEnvironmentValidator(
                environment,
                new JwtProperties(JWT_SECRET, 900000L, 604800000L),
                new CorsProperties(List.of("https://gout.example.com")),
                new UploadsProperties("/app/uploads"),
                crawlerProperties,
                anthropicProperties,
                openAiProperties
        );
    }
}

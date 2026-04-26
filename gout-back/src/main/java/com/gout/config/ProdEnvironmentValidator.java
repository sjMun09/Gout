package com.gout.config;

import com.gout.config.properties.AnthropicProperties;
import com.gout.config.properties.CorsProperties;
import com.gout.config.properties.CrawlerProperties;
import com.gout.config.properties.JwtProperties;
import com.gout.config.properties.OpenAiProperties;
import com.gout.config.properties.UploadsProperties;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ProdEnvironmentValidator implements InitializingBean {

    private final Environment environment;
    private final JwtProperties jwtProperties;
    private final CorsProperties corsProperties;
    private final UploadsProperties uploadsProperties;
    private final CrawlerProperties crawlerProperties;
    private final AnthropicProperties anthropicProperties;
    private final OpenAiProperties openAiProperties;

    public ProdEnvironmentValidator(
            Environment environment,
            JwtProperties jwtProperties,
            CorsProperties corsProperties,
            UploadsProperties uploadsProperties,
            CrawlerProperties crawlerProperties,
            AnthropicProperties anthropicProperties,
            OpenAiProperties openAiProperties
    ) {
        this.environment = environment;
        this.jwtProperties = jwtProperties;
        this.corsProperties = corsProperties;
        this.uploadsProperties = uploadsProperties;
        this.crawlerProperties = crawlerProperties;
        this.anthropicProperties = anthropicProperties;
        this.openAiProperties = openAiProperties;
    }

    @Override
    public void afterPropertiesSet() {
        if (!environment.acceptsProfiles(Profiles.of("prod"))) {
            return;
        }

        List<String> errors = validate();
        if (!errors.isEmpty()) {
            throw new IllegalStateException("Production environment validation failed:\n- "
                    + String.join("\n- ", errors));
        }
    }

    List<String> validate() {
        List<String> errors = new ArrayList<>();

        requireProperty(errors, "spring.datasource.url", "DB_URL");
        requireProperty(errors, "spring.datasource.username", "DB_USERNAME");
        requireProperty(errors, "spring.datasource.password", "DB_PASSWORD");
        requireProperty(errors, "spring.data.redis.host", "REDIS_HOST");
        requirePositiveInteger(errors, "spring.data.redis.port", "REDIS_PORT");

        if (isBlank(jwtProperties.secret()) || jwtProperties.secret().length() < 32) {
            errors.add("JWT_SECRET must be set to at least 32 characters for jwt.secret");
        }

        validateCors(errors);

        if (isBlank(uploadsProperties.baseDir())) {
            errors.add("UPLOADS_BASE_DIR/app.uploads.base-dir must not be blank");
        }

        if (crawlerProperties.enabled()) {
            if (isBlank(anthropicProperties.apiKey())) {
                errors.add("PAPER_CRAWLER_ENABLED=true requires ANTHROPIC_API_KEY for app.anthropic.api-key");
            }
            if (isBlank(openAiProperties.apiKey())) {
                errors.add("PAPER_CRAWLER_ENABLED=true requires OPENAI_API_KEY for app.openai.api-key");
            }
        }

        return errors;
    }

    private void validateCors(List<String> errors) {
        List<String> allowedOrigins = corsProperties.allowedOrigins();
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            errors.add("CORS_ALLOWED_ORIGINS/app.cors.allowed-origins must include at least one explicit origin");
            return;
        }
        for (String origin : allowedOrigins) {
            if (isBlank(origin)) {
                errors.add("CORS_ALLOWED_ORIGINS/app.cors.allowed-origins must not contain blank entries");
            } else if ("*".equals(origin.trim())) {
                errors.add("CORS_ALLOWED_ORIGINS/app.cors.allowed-origins must not contain wildcard '*'");
            }
        }
    }

    private void requireProperty(List<String> errors, String propertyName, String envName) {
        if (isBlank(readProperty(propertyName))) {
            errors.add(envName + " must be set for " + propertyName);
        }
    }

    private void requirePositiveInteger(List<String> errors, String propertyName, String envName) {
        String value = readProperty(propertyName);
        if (isBlank(value)) {
            errors.add(envName + " must be set for " + propertyName);
            return;
        }
        try {
            if (Integer.parseInt(value.trim()) <= 0) {
                errors.add(envName + " must be a positive integer for " + propertyName);
            }
        } catch (NumberFormatException e) {
            errors.add(envName + " must be a positive integer for " + propertyName);
        }
    }

    private String readProperty(String propertyName) {
        try {
            return environment.getProperty(propertyName);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

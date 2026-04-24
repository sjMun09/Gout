package com.gout.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;

/**
 * Springdoc OpenAPI / Swagger UI 전용 설정.
 *
 * <p>{@link SecurityConfig} 는 Agent-I 가 수정 중이므로 충돌 회피를 위해 이 클래스에서만 담당한다.
 * WebSecurityCustomizer 로 `/v3/api-docs/**` 와 `/swagger-ui/**` 경로를
 * Spring Security 필터체인 자체에서 제외시키며(= 인증/필터 전부 bypass),
 * 동시에 {@link OpenAPI} 빈으로 JWT Bearer 스키마를 문서화한다.
 *
 * <p>운영 환경에서 Swagger UI 노출이 부담스럽다면
 * application-prod.yml 에서 {@code springdoc.swagger-ui.enabled: false} 로 끄면 된다.
 */
@Configuration
public class OpenApiWebSecurityCustomizer {

    private static final String[] OPENAPI_WHITELIST = new String[]{
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/v3/api-docs.yaml",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/webjars/**"   // swagger-ui 가 사용하는 webjars 리소스
    };

    /**
     * SecurityConfig 의 filterChain 은 건드리지 않고,
     * WebSecurity 단에서 Swagger UI 관련 경로를 완전히 무시(ignoring)하도록 한다.
     * JwtAuthenticationFilter 도 실행되지 않아 토큰 없어도 Swagger UI 접근 가능.
     */
    @Bean
    public WebSecurityCustomizer openApiIgnoringCustomizer() {
        return web -> web.ignoring().requestMatchers(OPENAPI_WHITELIST);
    }

    /**
     * OpenAPI 문서에 JWT Bearer 스키마를 기본으로 등록.
     * Controller 에 별도 어노테이션 없이도 각 엔드포인트가 "Authorize" 버튼으로 토큰 입력 가능하게 된다.
     */
    @Bean
    public OpenAPI goutOpenAPI() {
        final String bearerSchemeName = "BearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("Gout Care API")
                        .description("통풍 환자 포털 백엔드 API. `/api/auth/login` 으로 발급받은 "
                                + "access token 을 Authorize 버튼에 입력하면 인증 필요 엔드포인트 호출 가능.")
                        .version("v0.0.1"))
                .addSecurityItem(new SecurityRequirement().addList(bearerSchemeName))
                .components(new Components().addSecuritySchemes(bearerSchemeName,
                        new SecurityScheme()
                                .name(bearerSchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}

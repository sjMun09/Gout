package com.gout.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
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
        // SecurityScheme 의 스키마 이름은 OpenAPI document 의 components.securitySchemes 키와
        // SecurityRequirement.addList(name) 양쪽에서 사용된다.
        // Springdoc 컨벤션 + 본 프로젝트 통합 테스트 계약에 맞춰 소문자 "bearerAuth" 로 통일.
        final String bearerSchemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("Gout Care API")
                        .description("통풍 환자 포털 백엔드 API.\n\n"
                                + "- `/api/auth/login` 으로 발급받은 access token 을 Authorize 버튼에 입력하면 "
                                + "인증 필요 엔드포인트 호출 가능.\n"
                                + "- 표준 에러 응답: 모든 4xx/5xx 응답은 `ErrorResponse` 컴포넌트 참조 — "
                                + "`success`, `code`, `message`, `status`, `path`, `timestamp`, `fieldErrors` 필드를 갖는다.\n"
                                + "- 검증 실패는 422, 비즈니스 정책 위반은 400, 인증/권한은 401/403, "
                                + "리소스 없음은 404, 레이트 리미트는 429 로 매핑된다.")
                        .version("v0.0.1")
                        .contact(new Contact()
                                .name("Gout Care Team")
                                .url("https://github.com/sjMun09/Gout")
                                .email("noreply@example.com"))
                        .license(new License()
                                .name("Apache-2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .addSecurityItem(new SecurityRequirement().addList(bearerSchemeName))
                .components(new Components().addSecuritySchemes(bearerSchemeName,
                        new SecurityScheme()
                                .name(bearerSchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT access token. `/api/auth/login` 응답 `data.accessToken` 사용.")));
    }
}

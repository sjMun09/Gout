package com.gout.config;

import com.gout.security.JwtAuthenticationFilter;
import com.gout.security.JwtTokenProvider;
import com.gout.security.RateLimitFilter;
import com.gout.security.RestAccessDeniedHandler;
import com.gout.security.RestAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /** BCrypt 비용(strength). 2026년 권고 12. 값 ↑ → 단일 해시 비용 지수 증가. */
    private static final int BCRYPT_STRENGTH = 12;

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;
    private final RateLimitFilter rateLimitFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;
    private final com.gout.security.AdminTokenBlacklist adminTokenBlacklist;

    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 보안 헤더 최소 세트.
            // API 서버이므로 CSP 는 엄격. X-Frame-Options DENY. HSTS 1년.
            .headers(headers -> headers
                .contentTypeOptions(Customizer.withDefaults())
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000))
                .referrerPolicy(ref -> ref
                    .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN))
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'; frame-ancestors 'none'; base-uri 'none'"))
            )
            // 401 / 403 응답 포맷 통일.
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/admin/**").authenticated()
                // 건강 기록(요산수치/통풍발작/복약) — 민감 개인정보. 절대 permitAll 금지.
                // 명시적 authenticated() 로 의도 고정 (LOW-002 대응).
                .requestMatchers("/api/health/**").authenticated()
                .requestMatchers(
                    "/api/auth/**",
                    "/api/foods/**",
                    "/api/guidelines/**",
                    "/api/hospitals/**",
                    "/api/papers/**",
                    "/api/content/**",
                    "/actuator/health"
                ).permitAll()
                .requestMatchers(HttpMethod.GET, "/api/posts/**", "/api/comments/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/uploads/posts/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(
                new JwtAuthenticationFilter(jwtTokenProvider, userDetailsService, adminTokenBlacklist),
                UsernamePasswordAuthenticationFilter.class
            )
            // JwtAuthenticationFilter 뒤에 배치 — like 엔드포인트의 userId 키를 얻으려면 SecurityContext 가 이미 채워져 있어야 함.
            // login 엔드포인트는 인증 상태와 무관하게 IP 로 체크하므로 순서에 영향 없음.
            .addFilterAfter(rateLimitFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // strength 10 → 12 상향. 기존 10-cost 해시는 matches 는 통과하고
        // AuthServiceImpl.login 에서 upgradeEncoding 으로 자연 재해시된다.
        return new BCryptPasswordEncoder(BCRYPT_STRENGTH);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        // allowedHeaders "*" 는 운영 권고에 어긋남. 실제 사용 헤더만 명시.
        config.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "X-Requested-With",
                "Origin"));
        config.setExposedHeaders(List.of("Retry-After"));
        // LOW-003: JWT 를 Authorization 헤더(Bearer) 로만 주고받는 아키텍처에서는 allowCredentials=true
        // 가 의미가 없다. 쿠키/HTTP auth 쓰는 API 가 없으므로 false 로 고정해 설정을 단순화한다.
        // 결정 근거: docs/26-04-25_cors-credentials-decision.md (옵션 A).
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}

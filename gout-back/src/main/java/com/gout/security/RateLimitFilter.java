package com.gout.security;

import com.gout.global.response.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 레이트 리밋 필터.
 *
 * <p>세 엔드포인트를 인터셉트한다:
 * <ul>
 *   <li>POST /api/auth/login    — 키: 클라이언트 IP, 한도: 5 req/min (brute-force 방어)</li>
 *   <li>POST /api/auth/register — 키: 클라이언트 IP, 한도: 10 req/10min (CPU DoS + 계정 폭탄 방어)</li>
 *   <li>POST /api/posts/{id}/like — 키: 인증된 userId, 한도: 30 req/min (spam 방어)</li>
 * </ul>
 *
 * <p>버킷이 비면 429 Too Many Requests + 표준 ErrorResponse 본문 + Retry-After: 60 헤더를 반환.
 * <p>like 엔드포인트의 경우 JwtAuthenticationFilter 이후에 동작해야 SecurityContext 에서
 * userId 를 꺼낼 수 있으므로, SecurityConfig 에서 JwtAuthenticationFilter 뒤에 체인한다.
 */
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH    = "/api/auth/login";
    private static final String REGISTER_PATH = "/api/auth/register";
    private static final Pattern LIKE_PATH = Pattern.compile("^/api/posts/([^/]+)/like$");
    private static final long RETRY_AFTER_SECONDS = 60L;

    private final RateLimiterService rateLimiterService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (!HttpMethod.POST.matches(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();

        if (LOGIN_PATH.equals(path)) {
            String ip = resolveClientIp(request);
            if (!rateLimiterService.tryConsume("login:" + ip, RateLimiterService.LOGIN_BANDWIDTH)) {
                writeTooManyRequests(request, response, "로그인 시도가 너무 많습니다. 잠시 후 다시 시도해주세요.");
                return;
            }
            filterChain.doFilter(request, response);
            return;
        }

        if (REGISTER_PATH.equals(path)) {
            String ip = resolveClientIp(request);
            if (!rateLimiterService.tryConsume("register:" + ip, RateLimiterService.REGISTER_BANDWIDTH)) {
                writeTooManyRequests(request, response, "회원가입 시도가 너무 많습니다. 잠시 후 다시 시도해주세요.");
                return;
            }
            filterChain.doFilter(request, response);
            return;
        }

        Matcher likeMatcher = LIKE_PATH.matcher(path);
        if (likeMatcher.matches()) {
            String userId = resolveAuthenticatedUserId();
            // 비인증 요청은 레이트 리밋 대상 아님 — 뒤이은 Security 레이어에서 401/403 처리.
            if (userId != null) {
                if (!rateLimiterService.tryConsume("like:" + userId, RateLimiterService.LIKE_BANDWIDTH)) {
                    writeTooManyRequests(request, response, "좋아요 요청이 너무 많습니다. 잠시 후 다시 시도해주세요.");
                    return;
                }
            }
            filterChain.doFilter(request, response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 클라이언트 IP 해석 — TCP 소스 IP(`getRemoteAddr`) 만 신뢰한다.
     *
     * <p>X-Forwarded-For 를 애플리케이션에서 직접 파싱하지 않는 이유:
     * 신뢰할 수 없는 네트워크에서 들어온 요청이 XFF 를 위조해 매 요청마다 다른 IP 로
     * 가장할 수 있다(P1-11 취약점). 이 경우 login bucket 이 분리되어 rate limit 우회 + 크리덴셜 스터핑 가능.
     *
     * <p>실제 프록시(L4/L7 LB, Nginx, Ingress) 뒤에 배포할 때는 운영 환경에서
     * {@code server.forward-headers-strategy=NATIVE} + {@code server.tomcat.remoteip.internal-proxies}
     * 로 trusted proxy CIDR 을 명시해야 한다. 이 설정이 있으면 Tomcat {@code RemoteIpValve} 가
     * 신뢰 프록시로부터 전달된 XFF 를 검증해 {@code getRemoteAddr()} 에 원본 클라이언트 IP 를 주입한다.
     * 신뢰 프록시가 아닌 요청이 XFF 를 붙여도 무시된다 — 애플리케이션 코드는 변하지 않고 그대로 TCP 소스만 본다.
     *
     * <p>개발/테스트 프로필은 {@code forward-headers-strategy} 를 비활성(NONE) 으로 두어
     * XFF 위조 공격 시나리오를 그대로 재현할 수 있다.
     */
    private String resolveClientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    private String resolveAuthenticatedUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        Object principal = auth.getPrincipal();
        if (principal instanceof UserDetails ud) {
            return ud.getUsername();
        }
        if (principal instanceof String s && !"anonymousUser".equals(s)) {
            return s;
        }
        return null;
    }

    private void writeTooManyRequests(HttpServletRequest request,
                                      HttpServletResponse response,
                                      String message) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(RETRY_AFTER_SECONDS));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.TOO_MANY_REQUESTS.value(),
                "TOO_MANY_REQUESTS",
                message,
                request.getRequestURI()
        );
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}

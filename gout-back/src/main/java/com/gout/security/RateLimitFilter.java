package com.gout.security;

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
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 레이트 리밋 필터.
 *
 * <p>두 엔드포인트를 인터셉트한다:
 * <ul>
 *   <li>POST /api/auth/login — 키: 클라이언트 IP (X-Forwarded-For 폴백)</li>
 *   <li>POST /api/posts/{id}/like — 키: 인증된 userId</li>
 * </ul>
 *
 * <p>버킷이 비면 429 Too Many Requests + ApiResponse 에러 본문 + Retry-After: 60 헤더를 반환.
 * <p>like 엔드포인트의 경우 JwtAuthenticationFilter 이후에 동작해야 SecurityContext 에서
 * userId 를 꺼낼 수 있으므로, SecurityConfig 에서 JwtAuthenticationFilter 뒤에 체인한다.
 */
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/auth/login";
    private static final Pattern LIKE_PATH = Pattern.compile("^/api/posts/([^/]+)/like$");
    private static final long RETRY_AFTER_SECONDS = 60L;

    private final RateLimiterService rateLimiterService;

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
                writeTooManyRequests(response, "로그인 시도가 너무 많습니다. 잠시 후 다시 시도해주세요.");
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
                    writeTooManyRequests(response, "좋아요 요청이 너무 많습니다. 잠시 후 다시 시도해주세요.");
                    return;
                }
            }
            filterChain.doFilter(request, response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * X-Forwarded-For 가 있으면 첫 번째 IP, 없으면 remoteAddr.
     * 프록시/로드밸런서 뒤에 있을 때 신뢰 가능한 XFF 설정은 별도 트러스티드 프록시 구성이 필요하나,
     * 현 단계에서는 헤더 우선 + remote addr 폴백 정책만 적용.
     */
    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xff)) {
            int comma = xff.indexOf(',');
            return (comma > 0 ? xff.substring(0, comma) : xff).trim();
        }
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

    private void writeTooManyRequests(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(RETRY_AFTER_SECONDS));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        // GlobalExceptionHandler 의 ApiResponse.error 와 동일한 JSON 모양.
        String body = "{\"success\":false,\"message\":\"" + escape(message) + "\",\"data\":null}";
        response.getWriter().write(body);
    }

    private String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

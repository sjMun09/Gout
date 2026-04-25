package com.gout.security;

import com.gout.security.JwtTokenProvider.ParsedToken;
import com.gout.security.JwtTokenProvider.ValidationResult;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;

/**
 * Bearer access 토큰을 파싱해 SecurityContext 에 인증을 채우는 필터.
 *
 * <p>기존 구현은 만료/서명오류/형식오류를 모두 boolean 으로 흡수해 구분이 불가능했다.
 * P1-10 재설계 이후:
 * <ul>
 *   <li>typ=access 강제 — refresh 토큰이 Authorization 헤더에 들어오면 INVALID</li>
 *   <li>{@link io.jsonwebtoken.ExpiredJwtException} → request attr {@code auth.error=token_expired}</li>
 *   <li>그 외 JwtException → {@code auth.error=invalid_token} + 보안 로그</li>
 *   <li>어떤 경우든 인증 미부여 후 체인 진행 — 최종 401 은 {@link RestAuthenticationEntryPoint} 에서 내려간다</li>
 * </ul>
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String ATTR_AUTH_ERROR = "auth.error";
    public static final String ERROR_EXPIRED = "token_expired";
    public static final String ERROR_INVALID = "invalid_token";

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;
    private final AdminTokenBlacklist adminTokenBlacklist;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);

        if (StringUtils.hasText(token)) {
            try {
                ParsedToken parsed = jwtTokenProvider.parseAccess(token);
                // MED-004: ADMIN 토큰은 logout 시 jti 블랙리스트에 올라간다. 남은 수명 동안
                // 블랙리스트된 access 는 즉시 차단. 일반 유저 토큰은 검사하지 않아
                // hot path 에 추가 Redis 라운드트립이 생기지 않는다.
                if (hasAdminRole(parsed) && adminTokenBlacklist.isRevoked(parsed.getJti())) {
                    request.setAttribute(ATTR_AUTH_ERROR, ERROR_INVALID);
                    log.warn("Blacklisted ADMIN access token jti={} from {} ({})",
                            parsed.getJti(), request.getRemoteAddr(), request.getRequestURI());
                    SecurityContextHolder.clearContext();
                } else {
                    authenticate(parsed, request);
                }
            } catch (JwtException e) {
                ValidationResult result = JwtTokenProvider.classify(e);
                if (result == ValidationResult.EXPIRED) {
                    request.setAttribute(ATTR_AUTH_ERROR, ERROR_EXPIRED);
                    // 만료는 운영 중 흔한 정상 흐름 — debug 수준.
                    log.debug("Access token expired: {}", e.getMessage());
                } else {
                    request.setAttribute(ATTR_AUTH_ERROR, ERROR_INVALID);
                    // 서명/형식/typ 오류는 보안 이벤트로 남긴다.
                    log.warn("Invalid access token from {} ({}): {}",
                            request.getRemoteAddr(), request.getRequestURI(), e.getMessage());
                }
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(ParsedToken parsed, HttpServletRequest request) {
        String userId = parsed.getUserId();
        try {
            UserDetails userDetails = userDetailsService.loadUserByUsername(userId);
            Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
            // 토큰의 roles 를 참고하되, DB 상태(삭제/정지)를 최종 권위로 둔다.
            // roles 가 없으면 UserDetails 의 authorities 를 그대로 사용.
            if (parsed.getRoles() != null && !parsed.getRoles().isEmpty() && authorities.isEmpty()) {
                authorities = parsed.getRoles().stream()
                        .map(r -> new SimpleGrantedAuthority(r.startsWith("ROLE_") ? r : "ROLE_" + r))
                        .map(GrantedAuthority.class::cast)
                        .toList();
            }
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (UsernameNotFoundException e) {
            // 탈퇴(DELETED) / 삭제된 사용자 — 인증 부여하지 않고 진행.
            // EntryPoint 가 401 invalid_token 로 내려준다.
            request.setAttribute(ATTR_AUTH_ERROR, ERROR_INVALID);
            SecurityContextHolder.clearContext();
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }

    /** 토큰의 roles 에 ADMIN 권한이 섞여 있는지. "ROLE_ADMIN" / "ADMIN" 둘 다 인정. */
    static boolean hasAdminRole(ParsedToken parsed) {
        if (parsed == null || parsed.getRoles() == null) {
            return false;
        }
        for (String r : parsed.getRoles()) {
            if (r == null) continue;
            String normalized = r.startsWith("ROLE_") ? r.substring(5) : r;
            if ("ADMIN".equalsIgnoreCase(normalized)) {
                return true;
            }
        }
        return false;
    }
}

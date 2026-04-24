package com.gout.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * JWT 발급/검증 (P1-10 ~ P1-12 재설계).
 *
 * <h3>클레임 표준</h3>
 * <ul>
 *   <li>iss: gout-care</li>
 *   <li>aud: gout-api</li>
 *   <li>sub: userId</li>
 *   <li>iat / nbf / exp</li>
 *   <li>jti: UUID v4 (access/refresh 모두 필수 — 재사용 탐지 키로 사용)</li>
 *   <li>typ: "access" | "refresh" (access 자리에 refresh 재사용 방지)</li>
 *   <li>roles: access 에만 포함 (refresh 에는 없음)</li>
 * </ul>
 *
 * <h3>파싱 시 예외 분기</h3>
 * <ul>
 *   <li>ExpiredJwtException → 만료 (AUTH_EXPIRED)</li>
 *   <li>SignatureException / MalformedJwtException / UnsupportedJwtException → 변조/형식 오류 (AUTH_INVALID)</li>
 *   <li>InvalidTokenTypeException(본 클래스 내부) → typ 불일치</li>
 * </ul>
 *
 * <p>clockSkewSeconds(30) 로 NTP drift 허용.
 */
@Slf4j
@Component
public class JwtTokenProvider {

    public static final String ISSUER = "gout-care";
    public static final String AUDIENCE = "gout-api";
    public static final long CLOCK_SKEW_SECONDS = 30L;

    public static final String TYP_ACCESS = "access";
    public static final String TYP_REFRESH = "refresh";

    private static final String CLAIM_TYP = "typ";
    private static final String CLAIM_ROLES = "roles";

    private final SecretKey secretKey;
    private final long accessTokenExpiry;
    private final long refreshTokenExpiry;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiry}") long accessTokenExpiry,
            @Value("${jwt.refresh-token-expiry}") long refreshTokenExpiry) {
        // HS256 은 최소 256비트(32바이트) 키 필수. 짧으면 jjwt 가 런타임 예외 → 기동 시 선제 검증.
        // application.yml 에서 기본값을 제거했으므로 JWT_SECRET 미설정이면 Spring 이 placeholder resolution 단계에서 이미 실패하지만,
        // 32바이트 미달 값이 주입된 경우를 대비한 방어선.
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "jwt.secret must be configured. Set JWT_SECRET environment variable.");
        }
        if (secret.getBytes().length < 32) {
            throw new IllegalStateException(
                    "jwt.secret must be at least 32 bytes (256 bits) for HS256.");
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessTokenExpiry = accessTokenExpiry;
        this.refreshTokenExpiry = refreshTokenExpiry;
    }

    // =====================================================================
    //  발급
    // =====================================================================

    public String generateAccessToken(String userId, String role) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + accessTokenExpiry);
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(ISSUER)
                .audience().add(AUDIENCE).and()
                .subject(userId)
                .issuedAt(now)
                .notBefore(now)
                .expiration(exp)
                .claim(CLAIM_TYP, TYP_ACCESS)
                .claim(CLAIM_ROLES, List.of(role == null ? "USER" : role))
                .signWith(secretKey)
                .compact();
    }

    public String generateRefreshToken(String userId) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + refreshTokenExpiry);
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(ISSUER)
                .audience().add(AUDIENCE).and()
                .subject(userId)
                .issuedAt(now)
                .notBefore(now)
                .expiration(exp)
                .claim(CLAIM_TYP, TYP_REFRESH)
                .signWith(secretKey)
                .compact();
    }

    // =====================================================================
    //  파싱 (typ 별 분리)
    // =====================================================================

    /** access 토큰을 파싱. typ=access 가 아니면 {@link InvalidTokenTypeException}. */
    public ParsedToken parseAccess(String token) {
        Claims claims = parseSignedClaims(token);
        requireType(claims, TYP_ACCESS);
        return ParsedToken.of(claims);
    }

    /** refresh 토큰을 파싱. typ=refresh 가 아니면 {@link InvalidTokenTypeException}. */
    public ParsedToken parseRefresh(String token) {
        Claims claims = parseSignedClaims(token);
        requireType(claims, TYP_REFRESH);
        return ParsedToken.of(claims);
    }

    private Claims parseSignedClaims(String token) {
        // jjwt 는 iss / aud 를 require 하면 불일치 시 IncorrectClaimException(= JwtException 하위) 를 던진다.
        return Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer(ISSUER)
                .requireAudience(AUDIENCE)
                .clockSkewSeconds(CLOCK_SKEW_SECONDS)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private void requireType(Claims claims, String expected) {
        Object actual = claims.get(CLAIM_TYP);
        if (!expected.equals(actual)) {
            throw new InvalidTokenTypeException(
                    "Expected typ=" + expected + " but was " + actual);
        }
    }

    public long getRefreshTokenExpirySeconds() {
        return refreshTokenExpiry / 1000L;
    }

    public long getAccessTokenExpirySeconds() {
        return accessTokenExpiry / 1000L;
    }

    /**
     * 예외 → 결과 구분. JwtAuthenticationFilter 가 만료 vs 변조를 분기하는 데 사용.
     */
    public static ValidationResult classify(Throwable t) {
        if (t instanceof ExpiredJwtException) {
            return ValidationResult.EXPIRED;
        }
        if (t instanceof SignatureException
                || t instanceof MalformedJwtException
                || t instanceof UnsupportedJwtException
                || t instanceof InvalidTokenTypeException) {
            return ValidationResult.INVALID;
        }
        if (t instanceof JwtException) {
            // IncorrectClaimException(iss/aud 불일치 등) 포함
            return ValidationResult.INVALID;
        }
        return ValidationResult.INVALID;
    }

    // =====================================================================
    //  VO / 예외
    // =====================================================================

    public enum ValidationResult { EXPIRED, INVALID }

    @Getter
    public static final class ParsedToken {
        private final String userId;
        private final String jti;
        private final String typ;
        private final List<String> roles;

        private ParsedToken(String userId, String jti, String typ, List<String> roles) {
            this.userId = userId;
            this.jti = jti;
            this.typ = typ;
            this.roles = roles;
        }

        @SuppressWarnings("unchecked")
        static ParsedToken of(Claims claims) {
            Object rolesRaw = claims.get(CLAIM_ROLES);
            List<String> roles = (rolesRaw instanceof List<?> l)
                    ? l.stream().map(Object::toString).toList()
                    : List.of();
            return new ParsedToken(
                    claims.getSubject(),
                    claims.getId(),
                    (String) claims.get(CLAIM_TYP),
                    roles);
        }
    }

    /** typ 클레임이 기대값과 다를 때. JwtException 하위라 기존 catch 흐름과 호환. */
    public static class InvalidTokenTypeException extends JwtException {
        public InvalidTokenTypeException(String msg) { super(msg); }
    }
}

package com.gout.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenExpiry;
    private final long refreshTokenExpiry;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiry}") long accessTokenExpiry,
            @Value("${jwt.refresh-token-expiry}") long refreshTokenExpiry) {
        // HS256 은 최소 256비트(32바이트) 키 필수. 짧으면 jjwt 가 런타임 예외 → 기동 시 선제 검증.
        if (secret == null || secret.getBytes().length < 32) {
            throw new IllegalStateException(
                    "jwt.secret must be at least 32 bytes (256 bits) for HS256. "
                            + "Set JWT_SECRET environment variable.");
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessTokenExpiry = accessTokenExpiry;
        this.refreshTokenExpiry = refreshTokenExpiry;
    }

    public String generateAccessToken(String userId, String email) {
        return Jwts.builder()
                .subject(userId)
                .claim("email", email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpiry))
                .signWith(secretKey)
                .compact();
    }

    public String generateRefreshToken(String userId) {
        // jti(random UUID) 를 넣어 매 발급마다 다른 토큰을 보장한다.
        // iat 이 초 단위라 1초 내 연속 재발급 시 동일 토큰이 나올 수 있는데,
        // Redis 로테이션 시 이전 토큰과 구분이 안 되면 탈취 재사용을 막을 수 없다.
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(userId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshTokenExpiry))
                .signWith(secretKey)
                .compact();
    }

    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String getUserId(String token) {
        return getClaims(token).getSubject();
    }

    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 리프레시 토큰 만료(ms) → 초 단위 TTL. RefreshTokenStore 의 EX 파라미터로 사용.
     * JWT 자체 만료와 Redis 키 TTL 을 맞춰 토큰 수명 초과 후에도 키가 남지 않도록 한다.
     */
    public long getRefreshTokenExpirySeconds() {
        return refreshTokenExpiry / 1000L;
    }
}

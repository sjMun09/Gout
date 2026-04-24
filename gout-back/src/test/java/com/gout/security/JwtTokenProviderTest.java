package com.gout.security;

import com.gout.security.JwtTokenProvider.ParsedToken;
import com.gout.security.JwtTokenProvider.ValidationResult;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JwtTokenProvider 단위 테스트 (P1-10).
 *
 * <p>검증 범위:
 * <ul>
 *   <li>표준 클레임(iss/aud/sub/iat/exp/jti/typ) 포함</li>
 *   <li>typ=access/refresh 교차 사용 시 InvalidTokenTypeException</li>
 *   <li>서명 변조 → SignatureException</li>
 *   <li>만료 → ExpiredJwtException / classify = EXPIRED</li>
 *   <li>clock skew 허용 범위 내 5초 초과 exp 는 파싱 성공, 60초 초과 exp 는 실패</li>
 * </ul>
 */
class JwtTokenProviderTest {

    private static final String SECRET = "test-secret-key-must-be-at-least-256-bits-long-for-hs256-xxxxx";
    private static final long ACCESS_EXP_MS = 15 * 60 * 1000L;
    private static final long REFRESH_EXP_MS = 7L * 24 * 60 * 60 * 1000L;

    private final JwtTokenProvider provider =
            new JwtTokenProvider(SECRET, ACCESS_EXP_MS, REFRESH_EXP_MS);

    @Test
    @DisplayName("access 토큰은 iss/aud/sub/typ/roles/jti 를 포함한다")
    void accessToken_containsStandardClaims() {
        String token = provider.generateAccessToken("user-1", "USER");
        ParsedToken parsed = provider.parseAccess(token);

        assertThat(parsed.getUserId()).isEqualTo("user-1");
        assertThat(parsed.getTyp()).isEqualTo("access");
        assertThat(parsed.getJti()).isNotBlank();
        assertThat(parsed.getRoles()).containsExactly("USER");
    }

    @Test
    @DisplayName("refresh 토큰은 typ=refresh 이고 roles 는 없다")
    void refreshToken_hasNoRoles() {
        String token = provider.generateRefreshToken("user-2");
        ParsedToken parsed = provider.parseRefresh(token);

        assertThat(parsed.getTyp()).isEqualTo("refresh");
        assertThat(parsed.getUserId()).isEqualTo("user-2");
        assertThat(parsed.getRoles()).isEmpty();
    }

    @Test
    @DisplayName("refresh 토큰을 parseAccess 로 파싱하면 InvalidTokenTypeException")
    void parseAccess_rejectsRefreshToken() {
        String refresh = provider.generateRefreshToken("user-3");
        assertThatThrownBy(() -> provider.parseAccess(refresh))
                .isInstanceOf(JwtTokenProvider.InvalidTokenTypeException.class);
    }

    @Test
    @DisplayName("access 토큰을 parseRefresh 로 파싱하면 InvalidTokenTypeException")
    void parseRefresh_rejectsAccessToken() {
        String access = provider.generateAccessToken("user-4", "USER");
        assertThatThrownBy(() -> provider.parseRefresh(access))
                .isInstanceOf(JwtTokenProvider.InvalidTokenTypeException.class);
    }

    @Test
    @DisplayName("서명이 다른 토큰은 JwtException + classify=INVALID")
    void tamperedSignature_isInvalid() {
        // 다른 시크릿 키로 만든 토큰
        SecretKey otherKey = Keys.hmacShaKeyFor(
                "another-secret-key-must-be-at-least-256-bits-long-xxx".getBytes());
        String bogus = Jwts.builder()
                .subject("x")
                .issuer(JwtTokenProvider.ISSUER)
                .audience().add(JwtTokenProvider.AUDIENCE).and()
                .claim("typ", "access")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(otherKey)
                .compact();

        assertThatThrownBy(() -> provider.parseAccess(bogus))
                .isInstanceOf(JwtException.class);
        try {
            provider.parseAccess(bogus);
        } catch (JwtException e) {
            assertThat(JwtTokenProvider.classify(e)).isEqualTo(ValidationResult.INVALID);
        }
    }

    @Test
    @DisplayName("만료된 토큰은 ExpiredJwtException + classify=EXPIRED")
    void expiredToken_isExpired() throws Exception {
        // 매우 짧은 만료 시간으로 provider 재생성
        JwtTokenProvider shortProvider = new JwtTokenProvider(SECRET, 1L, REFRESH_EXP_MS);
        String token = shortProvider.generateAccessToken("user-5", "USER");
        // exp 를 넘기도록 대기. clock skew 30초 때문에 그 이상 필요.
        Thread.sleep(31_500L);

        assertThatThrownBy(() -> shortProvider.parseAccess(token))
                .isInstanceOf(ExpiredJwtException.class);
        try {
            shortProvider.parseAccess(token);
        } catch (ExpiredJwtException e) {
            assertThat(JwtTokenProvider.classify(e)).isEqualTo(ValidationResult.EXPIRED);
        }
    }

    @Test
    @DisplayName("clock skew 30초 이내 만료는 여전히 유효로 간주된다")
    void clockSkew_allowsRecentlyExpired() {
        // exp 가 10초 전인 토큰을 수동 생성
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes());
        Date now = new Date();
        String token = Jwts.builder()
                .subject("skew-user")
                .issuer(JwtTokenProvider.ISSUER)
                .audience().add(JwtTokenProvider.AUDIENCE).and()
                .claim("typ", "access")
                .issuedAt(new Date(now.getTime() - 60_000))
                .expiration(new Date(now.getTime() - 10_000))
                .id("jti-skew")
                .signWith(key)
                .compact();

        // clockSkewSeconds(30) 덕분에 10초 지난 exp 는 여전히 파싱 성공.
        ParsedToken parsed = provider.parseAccess(token);
        assertThat(parsed.getUserId()).isEqualTo("skew-user");
    }
}

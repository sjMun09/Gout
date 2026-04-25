package com.gout.security;

import com.gout.global.exception.BusinessException;
import com.gout.global.exception.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link CurrentUserProvider} 단위 테스트.
 *
 * <p>Spring 컨텍스트를 부팅하지 않고 {@link SecurityContextHolder} 를 직접 set/clear 하는 방식으로
 * principal 분기 로직만 검증한다.
 */
class CurrentUserProviderTest {

    private final CurrentUserProvider provider = new CurrentUserProvider();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Authentication 이 null 이면 findUserId 는 empty")
    void findUserId_returnsEmpty_whenAuthenticationIsNull() {
        SecurityContextHolder.clearContext();

        Optional<String> result = provider.findUserId();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("isAuthenticated=false 이면 findUserId 는 empty")
    void findUserId_returnsEmpty_whenNotAuthenticated() {
        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken("user-1", "pw");
        // authorities 미부여 → isAuthenticated()=false
        assertThat(token.isAuthenticated()).isFalse();
        SecurityContextHolder.getContext().setAuthentication(token);

        Optional<String> result = provider.findUserId();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("principal 이 UserDetails 면 username 을 반환")
    void findUserId_returnsUsername_whenPrincipalIsUserDetails() {
        UserDetails user = User.withUsername("user-1").password("pw").roles("USER").build();
        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(
                        user, null, AuthorityUtils.createAuthorityList("ROLE_USER"));
        SecurityContextHolder.getContext().setAuthentication(token);

        Optional<String> result = provider.findUserId();

        assertThat(result).contains("user-1");
    }

    @Test
    @DisplayName("principal 이 raw String 이면 그대로 반환")
    void findUserId_returnsString_whenPrincipalIsRawString() {
        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(
                        "user-2", null, AuthorityUtils.createAuthorityList("ROLE_USER"));
        SecurityContextHolder.getContext().setAuthentication(token);

        Optional<String> result = provider.findUserId();

        assertThat(result).contains("user-2");
    }

    @Test
    @DisplayName("principal 이 'anonymousUser' sentinel 이면 empty")
    void findUserId_returnsEmpty_whenPrincipalIsAnonymousString() {
        AnonymousAuthenticationToken token = new AnonymousAuthenticationToken(
                "key", "anonymousUser",
                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
        SecurityContextHolder.getContext().setAuthentication(token);

        Optional<String> result = provider.findUserId();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("requireUserId 는 미인증 시 BusinessException(UNAUTHORIZED) 을 던진다")
    void requireUserId_throwsUnauthorized_inAnonymousCase() {
        AnonymousAuthenticationToken token = new AnonymousAuthenticationToken(
                "key", "anonymousUser",
                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
        SecurityContextHolder.getContext().setAuthentication(token);

        assertThatThrownBy(provider::requireUserId)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("requireUserId 는 인증된 사용자의 username 을 반환")
    void requireUserId_returnsUsername_whenAuthenticated() {
        UserDetails user = User.withUsername("user-3").password("pw").roles("USER").build();
        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(
                        user, null, AuthorityUtils.createAuthorityList("ROLE_USER"));
        SecurityContextHolder.getContext().setAuthentication(token);

        assertThat(provider.requireUserId()).isEqualTo("user-3");
    }
}

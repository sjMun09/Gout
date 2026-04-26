package com.gout.security;

import com.gout.global.exception.BusinessException;
import com.gout.global.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * SecurityContext 에서 현재 인증된 사용자의 식별자(userId) 를 추출하는 단일 진입점.
 *
 * <p>도입 배경(#76): 컨트롤러/필터마다 동일한 SecurityContextHolder → Authentication →
 * principal 분기 로직이 중복되어 있었다. principal 타입이 환경마다 다른 점({@link UserDetails}
 * 또는 raw String) 과 미인증 시 sentinel {@code "anonymousUser"} 처리까지 매번 직접
 * 작성하던 것을 한 곳에 모은다.
 *
 * <p>외부 의존성 없음 — Spring SecurityContextHolder 만 사용하므로 단위 테스트에서
 * Spring 컨텍스트 부팅 없이 검증 가능.
 */
@Component
public class CurrentUserProvider {

    private static final String ANONYMOUS_USER = "anonymousUser";

    /**
     * 현재 사용자의 ID 를 반환. 미인증이면 {@link BusinessException}({@link ErrorCode#UNAUTHORIZED}) 을 던져
     * 글로벌 예외 핸들러를 통해 401 로 변환된다.
     */
    public String requireUserId() {
        return findUserId().orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }

    /**
     * 현재 사용자의 ID 를 Optional 로 반환. 미인증/익명이면 {@link Optional#empty()}.
     * permitAll 엔드포인트에서 "로그인했으면 그에 맞는 응답, 아니면 공개 응답" 분기에 사용.
     */
    public Optional<String> findUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return Optional.empty();
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof UserDetails ud) {
            return Optional.of(ud.getUsername());
        }
        if (principal instanceof String s && !ANONYMOUS_USER.equals(s)) {
            return Optional.of(s);
        }
        return Optional.empty();
    }
}

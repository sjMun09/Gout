package com.gout.service;

import com.gout.dao.UserRepository;
import com.gout.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 게시글/댓글/북마크 응답 변환 시 userId → nickname 을 해석하는 공통 유틸.
 *
 * V24 에서 DB FK 를 제거한 뒤, 다음 두 경우에 대해 "탈퇴한 사용자" 로 일관되게 표시한다.
 *   1) user row 자체가 없는 경우 (과거 고아 데이터 또는 향후 물리 삭제 시나리오)
 *   2) user.status == DELETED  (UserServiceImpl.withdraw 가 처리한 soft delete 상태)
 *
 * 이전에는 각 서비스가 "알 수 없음" 을 fallback 으로 썼으나, 탈퇴 사용자와
 * orphan 을 동일하게 취급해도 무방하고(UX 상 둘 다 "탈퇴한 사용자" 가 적절),
 * 각 서비스에 복붙된 loadNicknames 로직을 통합한다.
 */
@Component
@RequiredArgsConstructor
public class UserNicknameResolver {

    /** 프론트/응답에 일관되게 노출되는 탈퇴 사용자 표기. */
    public static final String WITHDRAWN_NICKNAME = "탈퇴한 사용자";

    private final UserRepository userRepository;

    /**
     * 주어진 userId 집합을 한 번의 findAllById 로 조회해 nickname 맵을 반환한다.
     * DELETED 상태 사용자는 "탈퇴한 사용자" 로 치환해 map 에 담는다.
     * 입력 집합에 포함됐으나 DB 에 없는 userId 는 map 에 미포함되며, 호출측은
     * {@link #resolve(Map, String)} 를 통해 동일 fallback 을 적용받는다.
     */
    public Map<String, String> loadNicknames(Set<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(
                        User::getId,
                        u -> u.getStatus() == User.Status.DELETED
                                ? WITHDRAWN_NICKNAME
                                : u.getNickname(),
                        (a, b) -> a));
    }

    /**
     * 단건 userId → nickname 해석. 존재하지 않거나 DELETED 면 "탈퇴한 사용자".
     */
    public String resolve(String userId) {
        if (userId == null) {
            return WITHDRAWN_NICKNAME;
        }
        return userRepository.findById(userId)
                .map(u -> u.getStatus() == User.Status.DELETED
                        ? WITHDRAWN_NICKNAME
                        : u.getNickname())
                .orElse(WITHDRAWN_NICKNAME);
    }

    /**
     * 호출측이 이미 배치 조회한 nicknameMap 을 갖고 있을 때 단건을 해석.
     */
    public String resolve(Map<String, String> nicknameMap, String userId) {
        if (userId == null) {
            return WITHDRAWN_NICKNAME;
        }
        return nicknameMap.getOrDefault(userId, WITHDRAWN_NICKNAME);
    }
}

package com.gout.dto.response;

import com.gout.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 관리자 유저 목록/상세용 응답 DTO.
 * status 는 Agent-H V23 이 users.status 컬럼을 추가한 뒤 실제 값이 채워진다.
 * 그 전에는 AdminServiceImpl 이 native SELECT 로 읽어서 넣는다.
 */
@Getter
@Builder
public class AdminUserResponse {

    private final String id;
    private final String email;
    private final String nickname;
    private final String role;
    private final String status;
    private final LocalDateTime createdAt;

    public static AdminUserResponse of(User user, String status) {
        return AdminUserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .role(user.getRole().name())
                .status(status != null ? status : User.Status.ACTIVE.name())
                .createdAt(user.getCreatedAt())
                .build();
    }
}

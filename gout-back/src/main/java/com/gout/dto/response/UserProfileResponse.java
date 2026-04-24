package com.gout.dto.response;

import com.gout.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * GET /api/me — 현재 로그인 사용자 프로필.
 */
@Getter
@AllArgsConstructor
public class UserProfileResponse {

    private String id;
    private String email;
    private String nickname;
    private User.Role role;
    private Integer birthYear;
    private User.Gender gender;
    private LocalDateTime createdAt;

    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getRole(),
                user.getBirthYear(),
                user.getGender(),
                user.getCreatedAt()
        );
    }
}

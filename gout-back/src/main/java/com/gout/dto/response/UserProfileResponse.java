package com.gout.dto.response;

import com.gout.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * GET /api/me — 현재 로그인 사용자 프로필.
 */
@Schema(description = "현재 로그인 사용자 프로필.")
@Getter
@AllArgsConstructor
public class UserProfileResponse {

    @Schema(description = "사용자 ID(ULID).", example = "01HABCDEFGHIJKLMNOPQRSTUVW")
    private String id;

    @Schema(description = "사용자 이메일.", example = "user@example.com")
    private String email;

    @Schema(description = "닉네임.", example = "통풍이")
    private String nickname;

    @Schema(description = "사용자 권한. USER 또는 ADMIN.", example = "USER")
    private User.Role role;

    @Schema(description = "출생연도(없으면 null).", example = "1990")
    private Integer birthYear;

    @Schema(description = "성별(없으면 null).", example = "MALE")
    private User.Gender gender;

    @Schema(description = "가입 시각(서버 기준 LocalDateTime).", example = "2026-01-01T00:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "민감 건강정보 수집·이용 동의 시각. 미동의 또는 철회 상태면 null.", example = "2026-01-01T00:00:00")
    private LocalDateTime consentSensitiveAt;

    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getRole(),
                user.getBirthYear(),
                user.getGender(),
                user.getCreatedAt(),
                user.getConsentSensitiveAt()
        );
    }
}

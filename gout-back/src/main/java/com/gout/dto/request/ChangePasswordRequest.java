package com.gout.dto.request;

import com.gout.global.validation.ValidPassword;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * POST /api/me/password — 비밀번호 변경 요청.
 */
@Getter
@Setter
@NoArgsConstructor
public class ChangePasswordRequest {

    @NotBlank(message = "현재 비밀번호는 필수입니다.")
    private String currentPassword;

    // P1-10: 비밀번호 정책 공통화.
    @NotBlank(message = "새 비밀번호는 필수입니다.")
    @ValidPassword
    private String newPassword;
}

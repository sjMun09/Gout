package com.gout.dto.request;

import com.gout.global.validation.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class RegisterRequest {

    @Email(message = "올바른 이메일 형식이 아닙니다.")
    @NotBlank(message = "이메일은 필수입니다.")
    private String email;

    // P1-10: @Size(min=8) → 10자 + 복잡성 + 공백 금지 정책으로 상향.
    // 기존 8자 계정은 로그인 시 재설정 유도(후속 티켓) — 이번 PR 에서는 신규/변경 경로만 강제.
    @NotBlank(message = "비밀번호는 필수입니다.")
    @ValidPassword
    private String password;

    @NotBlank(message = "닉네임은 필수입니다.")
    @Size(min = 2, max = 20, message = "닉네임은 2~20자 사이여야 합니다.")
    private String nickname;

    private Integer birthYear;
}

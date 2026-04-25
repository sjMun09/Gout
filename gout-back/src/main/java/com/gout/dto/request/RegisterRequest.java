package com.gout.dto.request;

import com.gout.global.validation.ValidPassword;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Schema(description = "회원가입 요청 본문.")
@Getter
public class RegisterRequest {

    @Schema(description = "사용자 이메일(고유).", example = "user@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    @NotBlank(message = "이메일은 필수입니다.")
    private String email;

    // P1-10: @Size(min=8) → 10자 + 복잡성 + 공백 금지 정책으로 상향.
    // 기존 8자 계정은 로그인 시 재설정 유도(후속 티켓) — 이번 PR 에서는 신규/변경 경로만 강제.
    @Schema(description = "비밀번호(평문). 10자 이상 + 영문/숫자/특수문자 조합 + 공백 금지.",
            example = "P@ssw0rd1234", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "비밀번호는 필수입니다.")
    @ValidPassword
    private String password;

    @Schema(description = "닉네임(2~20자).", example = "통풍이", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "닉네임은 필수입니다.")
    @Size(min = 2, max = 20, message = "닉네임은 2~20자 사이여야 합니다.")
    private String nickname;

    @Schema(description = "출생연도(선택).", example = "1990")
    private Integer birthYear;
}

package com.gout.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Schema(description = "로그인 요청 본문.")
@Getter
public class LoginRequest {

    @Schema(description = "사용자 이메일.", example = "user@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    @NotBlank(message = "이메일은 필수입니다.")
    private String email;

    @Schema(description = "비밀번호(평문).", example = "P@ssw0rd1234", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "비밀번호는 필수입니다.")
    private String password;
}

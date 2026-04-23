package com.gout.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class RefreshRequest {

    @NotBlank(message = "refresh token은 필수입니다.")
    private String refreshToken;
}

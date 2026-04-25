package com.gout.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "JWT 토큰 응답. 회원가입/로그인/재발급 모두 동일 shape.")
@Getter
public class TokenResponse {

    @Schema(description = "JWT access token. Authorization: Bearer 헤더에 사용.",
            example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyMSJ9.signature")
    private final String accessToken;

    @Schema(description = "JWT refresh token. /api/auth/refresh 호출 시 사용. 1회용(rotating).",
            example = "eyJhbGciOiJIUzI1NiJ9.eyJ0eXAiOiJyZWZyZXNoIn0.signature")
    private final String refreshToken;

    @Schema(description = "토큰 타입. 항상 'Bearer'.", example = "Bearer")
    private final String tokenType = "Bearer";

    public TokenResponse(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
}

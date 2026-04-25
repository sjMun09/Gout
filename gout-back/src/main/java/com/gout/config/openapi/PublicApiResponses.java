package com.gout.config.openapi;

import com.gout.global.response.ErrorResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 인증 불필요(또는 익명 호출 허용) 엔드포인트 표준 에러 응답 묶음.
 *
 * <p>401/403/404 같은 인증·권한·리소스 식별 관련 코드는 포함하지 않는다.
 * 401/404 가 함께 노출돼야 하는 엔드포인트는 인라인 {@code @ApiResponse} 로 추가하거나
 * {@link AuthenticatedApiResponses} 를 사용한다.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ApiResponse(
        responseCode = "400",
        description = "잘못된 요청 — 비즈니스 정책 위반.",
        content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                        name = "INVALID_INPUT",
                        value = "{\"success\":false,\"code\":\"COMMON_INVALID_INPUT\",\"message\":\"잘못된 입력값입니다.\",\"status\":400,\"path\":\"/api/auth/register\",\"timestamp\":\"2026-01-01T00:00:00Z\"}"
                )
        )
)
@ApiResponse(
        responseCode = "422",
        description = "검증 실패 — Bean Validation 필드 오류.",
        content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                        name = "VALIDATION_FAILED",
                        value = "{\"success\":false,\"code\":\"COMMON_INVALID_INPUT\",\"message\":\"입력값이 올바르지 않습니다.\",\"status\":422,\"path\":\"/api/auth/register\",\"timestamp\":\"2026-01-01T00:00:00Z\",\"fieldErrors\":[{\"field\":\"email\",\"code\":\"Email\",\"message\":\"올바른 이메일 형식이 아닙니다.\"}]}"
                )
        )
)
@ApiResponse(
        responseCode = "429",
        description = "요청 과다 — 레이트 리미트 초과.",
        content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                        name = "TOO_MANY_REQUESTS",
                        value = "{\"success\":false,\"code\":\"COMMON_TOO_MANY_REQUESTS\",\"message\":\"요청이 너무 많습니다. 잠시 후 다시 시도해주세요.\",\"status\":429,\"path\":\"/api/auth/login\",\"timestamp\":\"2026-01-01T00:00:00Z\"}"
                )
        )
)
@ApiResponse(
        responseCode = "500",
        description = "서버 오류.",
        content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                        name = "INTERNAL_SERVER_ERROR",
                        value = "{\"success\":false,\"code\":\"COMMON_INTERNAL_SERVER_ERROR\",\"message\":\"서버 오류가 발생했습니다.\",\"status\":500,\"path\":\"/api/posts\",\"timestamp\":\"2026-01-01T00:00:00Z\"}"
                )
        )
)
public @interface PublicApiResponses {
}

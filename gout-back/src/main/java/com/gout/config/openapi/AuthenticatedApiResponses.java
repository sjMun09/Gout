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
 * 인증 필요 엔드포인트 표준 에러 응답 묶음 — {@link StandardApiResponses} 와 동일한 응답 집합.
 *
 * <p>OpenAPI 메타-애노테이션이 다른 애노테이션을 합성하지 못하므로 별칭으로 만들지 않고
 * 같은 응답 정의를 그대로 중복해 둔다. 컨트롤러 코드에서 의도(인증 필요)가 더 명확히 드러나도록
 * 별도 이름으로 노출하는 것이 목적이다. {@code StandardApiResponses} 와 병기하면 응답이
 * 중복 등록되니 둘 중 하나만 붙인다.
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
                        value = "{\"success\":false,\"code\":\"COMMON_INVALID_INPUT\",\"message\":\"잘못된 입력값입니다.\",\"status\":400,\"path\":\"/api/posts\",\"timestamp\":\"2026-01-01T00:00:00Z\"}"
                )
        )
)
@ApiResponse(
        responseCode = "401",
        description = "인증 실패 — 토큰 누락/만료/위조.",
        content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                        name = "UNAUTHORIZED",
                        value = "{\"success\":false,\"code\":\"AUTH_UNAUTHORIZED\",\"message\":\"로그인이 필요합니다.\",\"status\":401,\"path\":\"/api/me\",\"timestamp\":\"2026-01-01T00:00:00Z\"}"
                )
        )
)
@ApiResponse(
        responseCode = "403",
        description = "권한 없음 — 인증은 됐지만 리소스 접근 불가(타인 리소스 / ADMIN 권한 부족 등).",
        content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                        name = "FORBIDDEN",
                        value = "{\"success\":false,\"code\":\"COMMON_FORBIDDEN\",\"message\":\"접근 권한이 없습니다.\",\"status\":403,\"path\":\"/api/admin/users\",\"timestamp\":\"2026-01-01T00:00:00Z\"}"
                )
        )
)
@ApiResponse(
        responseCode = "404",
        description = "리소스 없음.",
        content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                        name = "NOT_FOUND",
                        value = "{\"success\":false,\"code\":\"COMMON_NOT_FOUND\",\"message\":\"요청한 리소스를 찾을 수 없습니다.\",\"status\":404,\"path\":\"/api/posts/abc\",\"timestamp\":\"2026-01-01T00:00:00Z\"}"
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
                        value = "{\"success\":false,\"code\":\"COMMON_INVALID_INPUT\",\"message\":\"입력값이 올바르지 않습니다.\",\"status\":422,\"path\":\"/api/posts\",\"timestamp\":\"2026-01-01T00:00:00Z\",\"fieldErrors\":[{\"field\":\"title\",\"code\":\"NotBlank\",\"message\":\"제목은 필수입니다.\"}]}"
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
                        value = "{\"success\":false,\"code\":\"COMMON_TOO_MANY_REQUESTS\",\"message\":\"요청이 너무 많습니다. 잠시 후 다시 시도해주세요.\",\"status\":429,\"path\":\"/api/posts/like\",\"timestamp\":\"2026-01-01T00:00:00Z\"}"
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
public @interface AuthenticatedApiResponses {
}

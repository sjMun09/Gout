package com.gout.global.exception;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 모든 ErrorCode 항목이 외부 계약 규칙을 준수하는지 검증한다 (Issue #84).
 *
 * <p>code 필드는 프론트가 의존하는 안정적 식별자이므로 enum 식별자와 분리되어 있어야 하며,
 * 이름 규칙(대문자/숫자/언더스코어)과 enum 내 유일성을 강제한다.
 */
class ErrorCodeContractTest {

    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_]+$");

    @Test
    void every_error_code_has_non_blank_code() {
        for (ErrorCode ec : ErrorCode.values()) {
            assertThat(ec.getCode())
                    .as("ErrorCode.%s.code must be non-blank", ec.name())
                    .isNotBlank();
        }
    }

    @Test
    void every_error_code_matches_naming_pattern() {
        for (ErrorCode ec : ErrorCode.values()) {
            assertThat(ec.getCode())
                    .as("ErrorCode.%s.code (%s) must match %s",
                            ec.name(), ec.getCode(), CODE_PATTERN.pattern())
                    .matches(CODE_PATTERN);
        }
    }

    @Test
    void every_error_code_is_unique() {
        Set<String> seen = new HashSet<>();
        for (ErrorCode ec : ErrorCode.values()) {
            assertThat(seen.add(ec.getCode()))
                    .as("ErrorCode.%s.code (%s) must be unique across the enum",
                            ec.name(), ec.getCode())
                    .isTrue();
        }
    }

    @Test
    void every_error_code_has_status() {
        for (ErrorCode ec : ErrorCode.values()) {
            assertThat(ec.getStatus())
                    .as("ErrorCode.%s.status must be non-null", ec.name())
                    .isNotNull();
        }
    }
}

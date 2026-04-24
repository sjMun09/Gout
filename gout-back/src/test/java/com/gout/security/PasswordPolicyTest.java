package com.gout.security;

import com.gout.global.validation.PasswordPolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 비밀번호 정책 경계 테스트 (P1-10 / V16).
 */
class PasswordPolicyTest {

    @Test
    @DisplayName("null/empty 은 거부")
    void nullOrEmpty_isRejected() {
        assertThat(PasswordPolicy.isValid(null)).isFalse();
        assertThat(PasswordPolicy.isValid("")).isFalse();
    }

    @Test
    @DisplayName("9자는 거부, 10자 영+숫자는 통과")
    void lengthBoundary() {
        assertThat(PasswordPolicy.isValid("abcdefg12")).isFalse();   // 9자
        assertThat(PasswordPolicy.isValid("abcdefg123")).isTrue();   // 10자 영+숫
    }

    @Test
    @DisplayName("영문자만 있으면 거부 (1종)")
    void lettersOnly_isRejected() {
        assertThat(PasswordPolicy.isValid("abcdefghij")).isFalse();
    }

    @Test
    @DisplayName("숫자만 있으면 거부 (1종)")
    void digitsOnly_isRejected() {
        assertThat(PasswordPolicy.isValid("1234567890")).isFalse();
    }

    @Test
    @DisplayName("영문자+특수문자 2종은 통과")
    void letterPlusSpecial_isAccepted() {
        assertThat(PasswordPolicy.isValid("abcdef!@#$")).isTrue();
    }

    @Test
    @DisplayName("공백 포함 시 거부")
    void whitespace_isRejected() {
        assertThat(PasswordPolicy.isValid("abcde 1234")).isFalse();
        assertThat(PasswordPolicy.isValid("abcde\t1234")).isFalse();
    }

    @Test
    @DisplayName("72자 초과는 거부 (BCrypt 한계)")
    void over72_isRejected() {
        String pwd = "a1".repeat(37); // 74자
        assertThat(pwd.length()).isEqualTo(74);
        assertThat(PasswordPolicy.isValid(pwd)).isFalse();
    }

    @Test
    @DisplayName("딱 72자는 통과")
    void exactly72_isAccepted() {
        String pwd = "a1".repeat(36); // 72자
        assertThat(pwd.length()).isEqualTo(72);
        assertThat(PasswordPolicy.isValid(pwd)).isTrue();
    }
}

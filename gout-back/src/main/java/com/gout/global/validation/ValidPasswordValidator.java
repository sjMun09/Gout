package com.gout.global.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidPasswordValidator implements ConstraintValidator<ValidPassword, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // @NotBlank 는 별도 제약으로 처리한다 — 여기서는 null / blank 모두 정책 위반으로 본다.
        return PasswordPolicy.isValid(value);
    }
}

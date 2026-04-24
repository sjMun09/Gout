package com.gout.global.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 비밀번호 정책 제약 — {@link PasswordPolicy} 참조.
 *
 * <pre>
 * @ValidPassword
 * private String password;
 * </pre>
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidPasswordValidator.class)
public @interface ValidPassword {

    String message() default PasswordPolicy.MESSAGE;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

package com.gout.global.validation;

/**
 * 비밀번호 정책 상수 (P1-10).
 *
 * <ul>
 *   <li>최소 10자 / 최대 72자 (BCrypt 한계)</li>
 *   <li>공백 금지</li>
 *   <li>영문자 / 숫자 / 특수문자 중 2종 이상 포함</li>
 * </ul>
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 10;
    public static final int MAX_LENGTH = 72;

    public static final String MESSAGE =
            "비밀번호는 10~72자이며 영문·숫자·특수문자 중 2종 이상을 포함해야 합니다.";

    private PasswordPolicy() {}

    /**
     * 정책 검증. null / 공백 포함 / 길이 / 복잡도 요건을 한 번에 평가한다.
     * 테스트와 {@link ValidPasswordValidator} 양쪽에서 공유.
     */
    public static boolean isValid(String raw) {
        if (raw == null) return false;
        int len = raw.length();
        if (len < MIN_LENGTH || len > MAX_LENGTH) return false;
        boolean hasLetter = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;
        for (int i = 0; i < len; i++) {
            char c = raw.charAt(i);
            if (Character.isWhitespace(c)) return false;
            if (Character.isLetter(c)) hasLetter = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else hasSpecial = true;
        }
        int kinds = (hasLetter ? 1 : 0) + (hasDigit ? 1 : 0) + (hasSpecial ? 1 : 0);
        return kinds >= 2;
    }
}

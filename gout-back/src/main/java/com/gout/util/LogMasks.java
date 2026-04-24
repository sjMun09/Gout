package com.gout.util;

/**
 * PII 필드를 로그에 남길 때 사용하는 마스킹 헬퍼.
 * 런타임 오버헤드: 무시 가능(String.substring).
 */
public final class LogMasks {

    private LogMasks() {}

    /**
     * UUID/CUID 형태 userId 의 앞 6자 + tail 2자만 남기고 중간 마스킹.
     * 예) cm9abc123xyz → cm9abc****yz
     */
    public static String maskUserId(String userId) {
        if (userId == null || userId.length() < 10) return "***";
        return userId.substring(0, 6) + "****" + userId.substring(userId.length() - 2);
    }

    /**
     * JTI(UUID) 의 앞 8자만 노출하고 나머지 마스킹.
     * 예) 550e8400-e29b-41d4-... → 550e8400****
     */
    public static String maskJti(String jti) {
        if (jti == null || jti.length() < 8) return "***";
        return jti.substring(0, 8) + "****";
    }

    /**
     * email 의 local part 첫 2자 + ***@domain 형태로 마스킹.
     * 예) john.doe@example.com → jo***@example.com
     */
    public static String maskEmail(String email) {
        if (email == null) return "***";
        int at = email.indexOf('@');
        if (at < 0) return "***";
        String local = email.substring(0, at);
        String domain = email.substring(at); // @domain 포함
        if (local.length() <= 2) return local + "***" + domain;
        return local.substring(0, 2) + "***" + domain;
    }
}

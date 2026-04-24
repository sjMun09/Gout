package com.gout.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LogMasksTest {

    // ── maskUserId ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("maskUserId: null 입력 → ***")
    void maskUserId_null() {
        assertEquals("***", LogMasks.maskUserId(null));
    }

    @Test
    @DisplayName("maskUserId: 9자 이하 짧은 값 → ***")
    void maskUserId_tooShort() {
        assertEquals("***", LogMasks.maskUserId("short"));
        assertEquals("***", LogMasks.maskUserId("123456789")); // 9자
    }

    @Test
    @DisplayName("maskUserId: 정상 CUID(10자+) → 앞6 + **** + 뒤2")
    void maskUserId_normal() {
        // 길이 26 CUID 예시
        String userId = "cm9abc123xyzABCDEF012345";
        String masked = LogMasks.maskUserId(userId);
        assertTrue(masked.startsWith("cm9abc"), "앞 6자 유지");
        assertTrue(masked.endsWith("45"), "뒤 2자 유지");
        assertTrue(masked.contains("****"), "중간 마스킹");
        assertEquals("cm9abc****45", masked);
    }

    @Test
    @DisplayName("maskUserId: UUID 형태 (32자)")
    void maskUserId_uuid() {
        String userId = "550e8400e29b41d4a716446655440000";
        String masked = LogMasks.maskUserId(userId);
        assertEquals("550e84****00", masked);
    }

    // ── maskJti ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("maskJti: null 입력 → ***")
    void maskJti_null() {
        assertEquals("***", LogMasks.maskJti(null));
    }

    @Test
    @DisplayName("maskJti: 7자 이하 짧은 값 → ***")
    void maskJti_tooShort() {
        assertEquals("***", LogMasks.maskJti("1234567")); // 7자
    }

    @Test
    @DisplayName("maskJti: 정상 UUID → 앞 8자 + ****")
    void maskJti_normal() {
        String jti = "550e8400-e29b-41d4-a716-446655440000";
        String masked = LogMasks.maskJti(jti);
        assertEquals("550e8400****", masked);
    }

    // ── maskEmail ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("maskEmail: null 입력 → ***")
    void maskEmail_null() {
        assertEquals("***", LogMasks.maskEmail(null));
    }

    @Test
    @DisplayName("maskEmail: @ 없는 값 → ***")
    void maskEmail_noAt() {
        assertEquals("***", LogMasks.maskEmail("notanemail"));
    }

    @Test
    @DisplayName("maskEmail: 정상 이메일 → local 앞 2자 + ***@domain")
    void maskEmail_normal() {
        String masked = LogMasks.maskEmail("john.doe@example.com");
        assertEquals("jo***@example.com", masked);
    }

    @Test
    @DisplayName("maskEmail: local part 2자 이하 → local 전체 + ***@domain")
    void maskEmail_shortLocal() {
        String masked = LogMasks.maskEmail("ab@example.com");
        assertEquals("ab***@example.com", masked);
    }
}

package com.gout.constant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppConstantsTest {

    @Test
    @DisplayName("clampSize: 0 이하 → DEFAULT_PAGE_SIZE")
    void clampSize_nonPositive() {
        assertEquals(AppConstants.DEFAULT_PAGE_SIZE, AppConstants.clampSize(0));
        assertEquals(AppConstants.DEFAULT_PAGE_SIZE, AppConstants.clampSize(-1));
        assertEquals(AppConstants.DEFAULT_PAGE_SIZE, AppConstants.clampSize(Integer.MIN_VALUE));
    }

    @Test
    @DisplayName("clampSize: 1..MAX 범위 → 원본 유지")
    void clampSize_withinRange() {
        assertEquals(1, AppConstants.clampSize(1));
        assertEquals(AppConstants.DEFAULT_PAGE_SIZE,
                AppConstants.clampSize(AppConstants.DEFAULT_PAGE_SIZE));
        assertEquals(AppConstants.MAX_PAGE_SIZE, AppConstants.clampSize(AppConstants.MAX_PAGE_SIZE));
    }

    @Test
    @DisplayName("clampSize: MAX 초과 → MAX_PAGE_SIZE")
    void clampSize_aboveMax() {
        assertEquals(AppConstants.MAX_PAGE_SIZE, AppConstants.clampSize(AppConstants.MAX_PAGE_SIZE + 1));
        assertEquals(AppConstants.MAX_PAGE_SIZE, AppConstants.clampSize(10_000));
        assertEquals(AppConstants.MAX_PAGE_SIZE, AppConstants.clampSize(Integer.MAX_VALUE));
    }

    @Test
    @DisplayName("clampPage: 음수 → 0, 0·양수 → 원본")
    void clampPage() {
        assertEquals(0, AppConstants.clampPage(-1));
        assertEquals(0, AppConstants.clampPage(Integer.MIN_VALUE));
        assertEquals(0, AppConstants.clampPage(0));
        assertEquals(5, AppConstants.clampPage(5));
        assertEquals(Integer.MAX_VALUE, AppConstants.clampPage(Integer.MAX_VALUE));
    }
}

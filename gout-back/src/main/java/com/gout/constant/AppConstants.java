package com.gout.constant;

public final class AppConstants {

    private AppConstants() {}

    // HTTP
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    // 요산 수치 기준 (ACR 2020 가이드라인)
    public static final double URIC_ACID_NORMAL_MALE   = 7.0;
    public static final double URIC_ACID_NORMAL_FEMALE = 6.0;
    public static final double URIC_ACID_TARGET_GENERAL = 6.0;
    public static final double URIC_ACID_TARGET_SEVERE  = 5.0;

    // 병원 검색 기본 반경 (미터)
    public static final int DEFAULT_HOSPITAL_SEARCH_RADIUS = 3000;

    // 페이징
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE     = 100;
}

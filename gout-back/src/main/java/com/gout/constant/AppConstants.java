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

    // 페이징 — 공통 기본/상한.
    // 도메인별 상한은 PageablePolicy enum 의 정책으로 일원화한다(이슈 #74).
    // 아래 상수는 정책의 기준값이자, 정책 외 호출(인스턴스 생성 전 컴파일 타임)에서 참조될 수 있도록 보존한다.
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE     = 100;

    // 정책별 상한 — 현재 모두 동일(100)이지만, 향후 도메인별 차등이 필요할 때 이 상수만 변경하면 된다.
    // 임의 변경 금지 — PageablePolicy.* 의 maxSize 와 1:1 매핑.
    public static final int POST_MAX_PAGE_SIZE         = MAX_PAGE_SIZE;
    public static final int PAPER_MAX_PAGE_SIZE        = MAX_PAGE_SIZE;
    public static final int FOOD_MAX_PAGE_SIZE         = MAX_PAGE_SIZE;
    public static final int HOSPITAL_MAX_PAGE_SIZE     = MAX_PAGE_SIZE;
    public static final int BOOKMARK_MAX_PAGE_SIZE     = MAX_PAGE_SIZE;
    public static final int NOTIFICATION_MAX_PAGE_SIZE = MAX_PAGE_SIZE;

    /**
     * 페이지 size 파라미터를 [1, MAX_PAGE_SIZE] 구간으로 보정.
     * 0 이하는 DEFAULT_PAGE_SIZE, 상한 초과는 MAX_PAGE_SIZE.
     *
     * @deprecated 이슈 #74. 도메인별 정책은 {@link com.gout.global.page.PageablePolicy} 사용.
     *             기존 호출자(공통 기본 정책 의도) 호환을 위해 유지하되 신규 코드는 PageablePolicy 직접 사용.
     */
    @Deprecated
    public static int clampSize(int size) {
        if (size <= 0) return DEFAULT_PAGE_SIZE;
        return Math.min(size, MAX_PAGE_SIZE);
    }

    /**
     * 페이지 번호 보정: 음수는 0 으로. 상한은 두지 않는다(전체 레코드 수에 따라 정책 위임).
     *
     * @deprecated 이슈 #74. 도메인별 정책은 {@link com.gout.global.page.PageablePolicy} 사용.
     */
    @Deprecated
    public static int clampPage(int page) {
        return Math.max(page, 0);
    }
}

package com.gout.global.page;

import com.gout.constant.AppConstants;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * page/size 보정 정책을 한 곳으로 모은 유틸.
 *
 * <p>이슈 #74. 기존에 컨트롤러/서비스 곳곳에서 인라인 {@code Math.max} / {@code Math.min}
 * 으로 흩어져 있던 보정 로직을 정책 enum 으로 통합한다. 호출부는
 * {@code Policy.X.toPageable(page, size)} 한 줄로 안전한 {@link Pageable} 을 얻는다.
 *
 * <p>형태 결정: <b>enum 정책</b>. 도메인별 상한이 사실상 동일(20/100)하지만,
 * 향후 도메인별 상한이 갈라질 가능성에 대비해 정책 enum 으로 확장 여지를 둔다.
 * 호출부 시그니처는 {@code Policy.X.toPageable(...)} 단일 인터페이스로 통일한다.
 *
 * <p>page 음수 → 0, size 0 이하 → defaultSize, size &gt; maxSize → maxSize.
 */
public enum PageablePolicy {

    /** 기본 정책 (default 20 / max 100). 별도 정책이 없는 모든 도메인이 사용한다. */
    DEFAULT(AppConstants.DEFAULT_PAGE_SIZE, AppConstants.MAX_PAGE_SIZE),

    /** 게시글 목록 — 기본 정책과 동일하지만 의도 명시. */
    POST(AppConstants.DEFAULT_PAGE_SIZE, AppConstants.POST_MAX_PAGE_SIZE),

    /** 논문 목록 — 기본 정책과 동일. */
    PAPER(AppConstants.DEFAULT_PAGE_SIZE, AppConstants.PAPER_MAX_PAGE_SIZE),

    /** 음식 검색 — 기본 정책과 동일. */
    FOOD(AppConstants.DEFAULT_PAGE_SIZE, AppConstants.FOOD_MAX_PAGE_SIZE),

    /** 병원 검색/리뷰 — 기본 정책과 동일. */
    HOSPITAL(AppConstants.DEFAULT_PAGE_SIZE, AppConstants.HOSPITAL_MAX_PAGE_SIZE),

    /** 북마크 목록 — 기본 정책과 동일. */
    BOOKMARK(AppConstants.DEFAULT_PAGE_SIZE, AppConstants.BOOKMARK_MAX_PAGE_SIZE),

    /** 알림 목록 — 기본 정책과 동일. */
    NOTIFICATION(AppConstants.DEFAULT_PAGE_SIZE, AppConstants.NOTIFICATION_MAX_PAGE_SIZE);

    private final int defaultSize;
    private final int maxSize;

    PageablePolicy(int defaultSize, int maxSize) {
        this.defaultSize = defaultSize;
        this.maxSize = maxSize;
    }

    public int defaultSize() {
        return defaultSize;
    }

    public int maxSize() {
        return maxSize;
    }

    /** 음수 페이지 → 0 으로 보정. 상한은 두지 않는다 (전체 레코드 수 의존). */
    public int clampPage(int page) {
        return Math.max(page, 0);
    }

    /**
     * size 를 정책 [1, maxSize] 구간으로 보정. 0 이하는 정책의 defaultSize.
     */
    public int clampSize(int size) {
        if (size <= 0) return defaultSize;
        return Math.min(size, maxSize);
    }

    /** page/size 를 정책에 맞춰 보정한 {@link Pageable} 반환. */
    public Pageable toPageable(int page, int size) {
        return PageRequest.of(clampPage(page), clampSize(size));
    }

    /** sort 가 필요한 호출부용. */
    public Pageable toPageable(int page, int size, Sort sort) {
        return PageRequest.of(clampPage(page), clampSize(size), sort);
    }
}

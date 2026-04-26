package com.gout.global.page;

import com.gout.constant.AppConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * #74. PageablePolicy 가 모든 정책에서 일관된 page/size 보정을 보장하는지 검증.
 *
 * <p>설계 의도:
 * <ul>
 *   <li>음수 page → 0</li>
 *   <li>0 이하 size → defaultSize</li>
 *   <li>maxSize 초과 size → maxSize</li>
 *   <li>정상 범위는 그대로 통과</li>
 * </ul>
 */
class PageablePolicyTest {

    @ParameterizedTest(name = "[{0}] 음수 page → 0")
    @EnumSource(PageablePolicy.class)
    @DisplayName("clampPage: 음수는 0 으로")
    void clampPage_negative(PageablePolicy policy) {
        assertEquals(0, policy.clampPage(-1));
        assertEquals(0, policy.clampPage(Integer.MIN_VALUE));
    }

    @ParameterizedTest(name = "[{0}] 0/양수 page 는 그대로")
    @EnumSource(PageablePolicy.class)
    @DisplayName("clampPage: 0 이상은 원본 유지")
    void clampPage_nonNegative(PageablePolicy policy) {
        assertEquals(0, policy.clampPage(0));
        assertEquals(7, policy.clampPage(7));
        assertEquals(Integer.MAX_VALUE, policy.clampPage(Integer.MAX_VALUE));
    }

    @ParameterizedTest(name = "[{0}] 0 이하 size → defaultSize")
    @EnumSource(PageablePolicy.class)
    @DisplayName("clampSize: 0 이하는 정책의 defaultSize")
    void clampSize_nonPositive(PageablePolicy policy) {
        assertEquals(policy.defaultSize(), policy.clampSize(0));
        assertEquals(policy.defaultSize(), policy.clampSize(-1));
        assertEquals(policy.defaultSize(), policy.clampSize(Integer.MIN_VALUE));
    }

    @ParameterizedTest(name = "[{0}] 1..max 범위 size → 그대로")
    @EnumSource(PageablePolicy.class)
    @DisplayName("clampSize: 정상 범위는 원본 유지")
    void clampSize_withinRange(PageablePolicy policy) {
        assertEquals(1, policy.clampSize(1));
        assertEquals(policy.defaultSize(), policy.clampSize(policy.defaultSize()));
        assertEquals(policy.maxSize(), policy.clampSize(policy.maxSize()));
    }

    @ParameterizedTest(name = "[{0}] max 초과 size → maxSize")
    @EnumSource(PageablePolicy.class)
    @DisplayName("clampSize: maxSize 초과는 maxSize 로 절삭")
    void clampSize_aboveMax(PageablePolicy policy) {
        assertEquals(policy.maxSize(), policy.clampSize(policy.maxSize() + 1));
        assertEquals(policy.maxSize(), policy.clampSize(10_000));
        assertEquals(policy.maxSize(), policy.clampSize(Integer.MAX_VALUE));
    }

    @Test
    @DisplayName("toPageable: 비정상 값 입력에도 안전한 Pageable 반환")
    void toPageable_clampsBoth() {
        Pageable p = PageablePolicy.DEFAULT.toPageable(-5, 999_999);
        assertEquals(0, p.getPageNumber());
        assertEquals(PageablePolicy.DEFAULT.maxSize(), p.getPageSize());

        Pageable q = PageablePolicy.POST.toPageable(3, 0);
        assertEquals(3, q.getPageNumber());
        assertEquals(PageablePolicy.POST.defaultSize(), q.getPageSize());
    }

    @Test
    @DisplayName("toPageable(sort): Sort 가 보존된다")
    void toPageable_withSort_preservesSort() {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable p = PageablePolicy.NOTIFICATION.toPageable(0, 10, sort);
        assertNotNull(p.getSort());
        assertEquals(sort, p.getSort());
        assertEquals(0, p.getPageNumber());
        assertEquals(10, p.getPageSize());
    }

    @Nested
    @DisplayName("정책별 상한 — AppConstants 와 1:1 동기화 보장")
    class PolicyMaxAlignment {

        @Test
        void post_max() {
            assertEquals(AppConstants.POST_MAX_PAGE_SIZE, PageablePolicy.POST.maxSize());
        }

        @Test
        void paper_max() {
            assertEquals(AppConstants.PAPER_MAX_PAGE_SIZE, PageablePolicy.PAPER.maxSize());
        }

        @Test
        void food_max() {
            assertEquals(AppConstants.FOOD_MAX_PAGE_SIZE, PageablePolicy.FOOD.maxSize());
        }

        @Test
        void hospital_max() {
            assertEquals(AppConstants.HOSPITAL_MAX_PAGE_SIZE, PageablePolicy.HOSPITAL.maxSize());
        }

        @Test
        void bookmark_max() {
            assertEquals(AppConstants.BOOKMARK_MAX_PAGE_SIZE, PageablePolicy.BOOKMARK.maxSize());
        }

        @Test
        void notification_max() {
            assertEquals(AppConstants.NOTIFICATION_MAX_PAGE_SIZE, PageablePolicy.NOTIFICATION.maxSize());
        }

        @Test
        void default_max_matches_app_constants() {
            assertEquals(AppConstants.MAX_PAGE_SIZE, PageablePolicy.DEFAULT.maxSize());
            assertEquals(AppConstants.DEFAULT_PAGE_SIZE, PageablePolicy.DEFAULT.defaultSize());
        }

        @Test
        @DisplayName("모든 정책의 defaultSize 는 항상 1 이상 maxSize 이하")
        void all_policies_have_sane_bounds() {
            for (PageablePolicy p : PageablePolicy.values()) {
                assertTrue(p.defaultSize() >= 1,
                        "defaultSize must be >=1 but " + p + "=" + p.defaultSize());
                assertTrue(p.defaultSize() <= p.maxSize(),
                        "defaultSize must be <= maxSize but " + p + " default=" + p.defaultSize() + " max=" + p.maxSize());
                assertTrue(p.maxSize() >= 1, "maxSize must be >=1 but " + p + "=" + p.maxSize());
            }
        }
    }
}

package com.gout.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * #77 도메인 상태 enum 회귀 방지.
 *
 * <p>EnumType.STRING 매핑이라 enum.name() 이 DB 에 저장된다.
 * name() 이 바뀌면 운영 데이터와 매칭이 깨지므로 값 자체를 단언한다.
 */
class DomainStatusEnumTest {

    @Test
    @DisplayName("Post.Status — VISIBLE/HIDDEN/DELETED 3 값")
    void postStatusValues() {
        assertThat(Post.Status.values())
                .extracting(Enum::name)
                .containsExactly("VISIBLE", "HIDDEN", "DELETED");
    }

    @Test
    @DisplayName("Comment.Status — VISIBLE/DELETED 2 값")
    void commentStatusValues() {
        assertThat(Comment.Status.values())
                .extracting(Enum::name)
                .containsExactly("VISIBLE", "DELETED");
    }

    @Test
    @DisplayName("HospitalReview.Status — VISIBLE/HIDDEN/REPORTED 3 값")
    void hospitalReviewStatusValues() {
        assertThat(HospitalReview.Status.values())
                .extracting(Enum::name)
                .containsExactly("VISIBLE", "HIDDEN", "REPORTED");
    }

    @Test
    @DisplayName("Report.Status — PENDING/RESOLVED/DISMISSED 3 값")
    void reportStatusValues() {
        assertThat(Report.Status.values())
                .extracting(Enum::name)
                .containsExactly("PENDING", "RESOLVED", "DISMISSED");
    }

    @Test
    @DisplayName("Report.Status.isValid — null/empty/unknown false, 정의된 값 true")
    void reportStatusIsValid() {
        assertThat(Report.Status.isValid(null)).isFalse();
        assertThat(Report.Status.isValid("")).isFalse();
        assertThat(Report.Status.isValid("UNKNOWN")).isFalse();
        assertThat(Report.Status.isValid("PENDING")).isTrue();
        assertThat(Report.Status.isValid("RESOLVED")).isTrue();
        assertThat(Report.Status.isValid("DISMISSED")).isTrue();
    }
}

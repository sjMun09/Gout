package com.gout.dao;

import com.gout.entity.Food;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FoodRepository extends JpaRepository<Food, String> {

    // TODO: keyword null 바인딩 시 PG 드라이버가 bytea 로 타입 추론해 LOWER(bytea) 에러 → 500.
    //   CAST(:keyword AS string) 또는 빈 문자열 기본값 + LENGTH(:keyword) = 0 조건으로 재작성 필요.
    //   (docs/NEXT_STEPS.md §1.2 참조 — FoodIntegrationTest.search_foods 이 이슈로 @Disabled)
    @Query("SELECT f FROM Food f WHERE " +
           "(:keyword IS NULL OR LOWER(f.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "   OR LOWER(f.nameEn) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:purineLevel IS NULL OR f.purineLevel = :purineLevel) " +
           "AND (:category IS NULL OR f.category = :category)")
    Page<Food> search(@Param("keyword") String keyword,
                      @Param("purineLevel") Food.PurineLevel purineLevel,
                      @Param("category") String category,
                      Pageable pageable);

    @Query("SELECT DISTINCT f.category FROM Food f WHERE f.category IS NOT NULL ORDER BY f.category")
    List<String> findDistinctCategories();
}

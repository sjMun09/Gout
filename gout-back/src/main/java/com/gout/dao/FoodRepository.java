package com.gout.dao;

import com.gout.entity.Food;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FoodRepository extends JpaRepository<Food, String> {

    @Query("SELECT f FROM Food f WHERE " +
           "(LENGTH(:keyword) = 0 OR LOWER(f.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "   OR LOWER(f.nameEn) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (LENGTH(:purineLevel) = 0 OR CAST(f.purineLevel AS string) = :purineLevel) " +
           "AND (LENGTH(:category) = 0 OR f.category = :category)")
    Page<Food> search(@Param("keyword") String keyword,
                      @Param("purineLevel") String purineLevel,
                      @Param("category") String category,
                      Pageable pageable);

    @Query("SELECT DISTINCT f.category FROM Food f WHERE f.category IS NOT NULL ORDER BY f.category")
    List<String> findDistinctCategories();
}

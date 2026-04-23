package com.gout.dao;

import com.gout.entity.Guideline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GuidelineRepository extends JpaRepository<Guideline, String> {

    List<Guideline> findByIsPublishedTrueAndCategoryOrderByCreatedAtDesc(Guideline.GuidelineCategory category);

    List<Guideline> findByIsPublishedTrueAndTypeOrderByCreatedAtDesc(Guideline.GuidelineType type);

    List<Guideline> findByIsPublishedTrueOrderByCreatedAtDesc();

    @Query("SELECT g FROM Guideline g WHERE g.isPublished = true " +
           "AND (:category IS NULL OR g.category = :category) " +
           "AND (:type IS NULL OR g.type = :type) " +
           "ORDER BY g.createdAt DESC")
    List<Guideline> search(@Param("category") Guideline.GuidelineCategory category,
                           @Param("type") Guideline.GuidelineType type);
}

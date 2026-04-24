package com.gout.dao;

import com.gout.entity.Guideline;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GuidelineRepository extends JpaRepository<Guideline, String> {

    List<Guideline> findByIsPublishedTrueAndCategoryOrderByCreatedAtDesc(Guideline.GuidelineCategory category);

    List<Guideline> findByIsPublishedTrueAndTypeOrderByCreatedAtDesc(Guideline.GuidelineType type);

    List<Guideline> findByIsPublishedTrueAndCategoryAndTypeOrderByCreatedAtDesc(
            Guideline.GuidelineCategory category, Guideline.GuidelineType type);

    List<Guideline> findByIsPublishedTrueOrderByCreatedAtDesc();
}

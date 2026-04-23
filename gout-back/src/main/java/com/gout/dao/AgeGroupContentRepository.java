package com.gout.dao;

import com.gout.entity.AgeGroupContent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AgeGroupContentRepository extends JpaRepository<AgeGroupContent, String> {

    Optional<AgeGroupContent> findByAgeGroup(AgeGroupContent.AgeGroup ageGroup);
}

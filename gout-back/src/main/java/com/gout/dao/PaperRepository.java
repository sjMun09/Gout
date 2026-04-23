package com.gout.dao;

import com.gout.entity.Paper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaperRepository extends JpaRepository<Paper, String> {

    Page<Paper> findByCategoryOrderByPublishedAtDesc(String category, Pageable pageable);

    Page<Paper> findAllByOrderByPublishedAtDesc(Pageable pageable);

    Optional<Paper> findByPmid(String pmid);

    boolean existsByPmid(String pmid);
}

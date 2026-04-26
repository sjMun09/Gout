package com.gout.service.impl;

import com.gout.dao.PaperRepository;
import com.gout.dto.response.PaperResponse;
import com.gout.entity.Paper;
import com.gout.global.exception.BusinessException;
import com.gout.global.exception.ErrorCode;
import com.gout.global.page.PageablePolicy;
import com.gout.service.PaperService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaperServiceImpl implements PaperService {

    private final PaperRepository paperRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional(readOnly = true)
    public Page<PaperResponse> getPapers(String category, int page, int size) {
        Pageable pageable = PageablePolicy.PAPER.toPageable(page, size);

        Page<Paper> papers;
        if (category == null || category.isBlank()) {
            papers = paperRepository.findAllByOrderByPublishedAtDesc(pageable);
        } else {
            papers = paperRepository.findByCategoryOrderByPublishedAtDesc(category, pageable);
        }
        return papers.map(PaperResponse::of);
    }

    @Override
    @Transactional(readOnly = true)
    public PaperResponse findById(String id) {
        Paper paper = paperRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAPER_NOT_FOUND));
        return PaperResponse.of(paper);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaperResponse> findSimilar(String id, int limit) {
        // 기준 논문 존재 + embedding 보유 여부 확인
        Integer hasEmbedding = jdbcTemplate.query(
                "SELECT 1 FROM papers WHERE id = ? AND embedding IS NOT NULL LIMIT 1",
                rs -> rs.next() ? rs.getInt(1) : null,
                id
        );
        if (hasEmbedding == null) {
            // 기준 논문이 없거나 임베딩이 없으면 빈 리스트
            if (!paperRepository.existsById(id)) {
                throw new BusinessException(ErrorCode.PAPER_NOT_FOUND);
            }
            return List.of();
        }

        int safeLimit = limit <= 0 ? 5 : Math.min(limit, 50);

        // pgvector <=> = 코사인 거리
        List<String> similarIds = jdbcTemplate.query(
                """
                SELECT p.id FROM papers p
                WHERE p.id <> ? AND p.embedding IS NOT NULL
                ORDER BY p.embedding <=> (SELECT embedding FROM papers WHERE id = ?)
                LIMIT ?
                """,
                (rs, rowNum) -> rs.getString("id"),
                id, id, safeLimit
        );
        if (similarIds.isEmpty()) return List.of();

        // 순서 보존
        List<Paper> papers = paperRepository.findAllById(similarIds);
        // findAllById 는 순서 보장 안 됨 → 수동 정렬
        return similarIds.stream()
                .map(sid -> papers.stream().filter(p -> sid.equals(p.getId())).findFirst().orElse(null))
                .filter(p -> p != null)
                .map(PaperResponse::of)
                .toList();
    }
}

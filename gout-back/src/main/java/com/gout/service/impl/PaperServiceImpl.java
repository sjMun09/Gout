package com.gout.service.impl;

import com.gout.dao.PaperRepository;
import com.gout.dto.response.PaperResponse;
import com.gout.entity.Paper;
import com.gout.global.exception.BusinessException;
import com.gout.global.exception.ErrorCode;
import com.gout.service.PaperService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaperServiceImpl implements PaperService {

    private final PaperRepository paperRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<PaperResponse> getPapers(String category, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 20 : Math.min(size, 100);
        Pageable pageable = PageRequest.of(safePage, safeSize);

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
}

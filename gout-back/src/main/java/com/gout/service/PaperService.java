package com.gout.service;

import com.gout.dto.response.PaperResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface PaperService {

    Page<PaperResponse> getPapers(String category, int page, int size);

    PaperResponse findById(String id);

    /**
     * pgvector 코사인 거리 기반 유사 논문 조회.
     * 기준 논문의 embedding 이 NULL 이면 빈 리스트.
     */
    List<PaperResponse> findSimilar(String id, int limit);
}

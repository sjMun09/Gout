package com.gout.controller;

import com.gout.dao.PaperRepository;
import com.gout.entity.Paper;
import com.gout.global.exception.BusinessException;
import com.gout.global.exception.ErrorCode;
import com.gout.global.response.ApiResponse;
import com.gout.job.PaperCrawlerJob;
import com.gout.service.PaperAiService;
import com.gout.service.PaperEmbeddingService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 개발/운영 트리거용 관리자 엔드포인트.
 * SecurityConfig 에서 /api/admin/** 는 authenticated() 필수.
 * PaperCrawlerJob 은 app.crawler.enabled=true 일 때만 빈 생성되므로 ObjectProvider 로 지연 주입.
 */
@RestController
@RequestMapping("/api/admin/papers")
@RequiredArgsConstructor
public class AdminPaperController {

    private final ObjectProvider<PaperCrawlerJob> crawlerProvider;
    private final PaperAiService paperAiService;
    private final PaperEmbeddingService paperEmbeddingService;
    private final PaperRepository paperRepository;

    @PostMapping("/crawl")
    public ResponseEntity<ApiResponse<Map<String, Object>>> crawl() {
        PaperCrawlerJob job = crawlerProvider.getIfAvailable();
        if (job == null) {
            return ResponseEntity.ok(ApiResponse.success("crawler disabled (set app.crawler.enabled=true)", Map.of(
                    "enabled", false,
                    "saved", 0
            )));
        }
        int saved = job.run();
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "enabled", true,
                "saved", saved
        )));
    }

    @PostMapping("/{id}/embed")
    public ResponseEntity<ApiResponse<Map<String, Object>>> embed(@PathVariable String id) {
        if (!paperRepository.existsById(id)) {
            throw new BusinessException(ErrorCode.PAPER_NOT_FOUND);
        }
        boolean ok = paperEmbeddingService.embedPaper(id);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "id", id,
                "updated", ok
        )));
    }

    @PostMapping("/{id}/summarize")
    public ResponseEntity<ApiResponse<Map<String, Object>>> summarize(@PathVariable String id) {
        Paper paper = paperRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAPER_NOT_FOUND));
        PaperAiService.SummaryResult summary = paperAiService.summarize(paper.getTitle(), paper.getAbstractEn());
        boolean updated = false;
        if (summary != null) {
            paper.updateSummary(summary.abstractKo, summary.aiSummaryKo);
            paperRepository.save(paper);
            updated = true;
        }
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "id", id,
                "updated", updated
        )));
    }
}

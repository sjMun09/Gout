package com.gout.controller;

import com.gout.dto.response.PaperResponse;
import com.gout.global.response.ApiResponse;
import com.gout.service.PaperService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/papers")
@RequiredArgsConstructor
public class PaperController {

    private final PaperService paperService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<PaperResponse>>> getPapers(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(paperService.getPapers(category, page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PaperResponse>> getPaper(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(paperService.findById(id)));
    }

    @GetMapping("/{id}/similar")
    public ResponseEntity<ApiResponse<List<PaperResponse>>> findSimilar(
            @PathVariable String id,
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(ApiResponse.success(paperService.findSimilar(id, limit)));
    }
}

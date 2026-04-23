package com.gout.controller;

import com.gout.dto.response.GuidelineResponse;
import com.gout.global.response.ApiResponse;
import com.gout.service.GuidelineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/guidelines")
@RequiredArgsConstructor
public class GuidelineController {

    private final GuidelineService guidelineService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<GuidelineResponse>>> getGuidelines(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String type) {
        return ResponseEntity.ok(ApiResponse.success(guidelineService.getGuidelines(category, type)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GuidelineResponse>> getGuideline(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(guidelineService.findById(id)));
    }
}

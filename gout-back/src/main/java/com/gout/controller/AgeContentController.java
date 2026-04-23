package com.gout.controller;

import com.gout.dto.response.AgeGroupContentResponse;
import com.gout.global.response.ApiResponse;
import com.gout.service.AgeContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/content")
@RequiredArgsConstructor
public class AgeContentController {

    private final AgeContentService ageContentService;

    @GetMapping("/age-group")
    public ResponseEntity<ApiResponse<List<AgeGroupContentResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(ageContentService.getAll()));
    }

    @GetMapping("/age-group/{group}")
    public ResponseEntity<ApiResponse<AgeGroupContentResponse>> getByAgeGroup(@PathVariable String group) {
        return ResponseEntity.ok(ApiResponse.success(ageContentService.getByAgeGroup(group)));
    }
}

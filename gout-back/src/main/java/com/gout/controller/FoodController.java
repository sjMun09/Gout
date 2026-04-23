package com.gout.controller;

import com.gout.dto.request.FoodSearchRequest;
import com.gout.dto.response.FoodResponse;
import com.gout.global.response.ApiResponse;
import com.gout.service.FoodService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/foods")
@RequiredArgsConstructor
public class FoodController {

    private final FoodService foodService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<FoodResponse>>> search(@ModelAttribute FoodSearchRequest request) {
        return ResponseEntity.ok(ApiResponse.success(foodService.search(request)));
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<String>>> getCategories() {
        return ResponseEntity.ok(ApiResponse.success(foodService.getCategories()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FoodResponse>> findById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(foodService.findById(id)));
    }
}

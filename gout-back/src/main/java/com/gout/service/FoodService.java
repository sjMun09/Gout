package com.gout.service;

import com.gout.dto.request.FoodSearchRequest;
import com.gout.dto.response.FoodResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface FoodService {

    Page<FoodResponse> search(FoodSearchRequest request);

    FoodResponse findById(String id);

    List<String> getCategories();
}

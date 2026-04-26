package com.gout.service.impl;

import com.gout.dao.FoodRepository;
import com.gout.dto.request.FoodSearchRequest;
import com.gout.dto.response.FoodResponse;
import com.gout.entity.Food;
import com.gout.global.exception.BusinessException;
import com.gout.global.exception.ErrorCode;
import com.gout.global.page.PageablePolicy;
import com.gout.service.FoodService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FoodServiceImpl implements FoodService {

    private final FoodRepository foodRepository;

    @Override
    public Page<FoodResponse> search(FoodSearchRequest request) {
        String keyword = StringUtils.hasText(request.getKeyword()) ? request.getKeyword().trim() : "";
        String category = StringUtils.hasText(request.getCategory()) ? request.getCategory().trim() : "";
        Food.PurineLevel purineLevelEnum = parsePurineLevel(request.getPurineLevel());
        String purineLevel = purineLevelEnum != null ? purineLevelEnum.name() : "";

        // #74: 기존엔 size 상한이 없어 거대 size 요청에 노출됐다. PageablePolicy.FOOD 로 상한 도입.
        Pageable pageable = PageablePolicy.FOOD.toPageable(
                request.getPage(),
                request.getSize(),
                Sort.by(Sort.Direction.ASC, "name"));

        return foodRepository.search(keyword, purineLevel, category, pageable)
                .map(FoodResponse::of);
    }

    @Override
    public FoodResponse findById(String id) {
        Food food = foodRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.FOOD_NOT_FOUND));
        return FoodResponse.of(food);
    }

    @Override
    public List<String> getCategories() {
        return foodRepository.findDistinctCategories();
    }

    private Food.PurineLevel parsePurineLevel(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return Food.PurineLevel.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "purineLevel은 LOW, MEDIUM, HIGH, VERY_HIGH 중 하나여야 합니다.");
        }
    }
}

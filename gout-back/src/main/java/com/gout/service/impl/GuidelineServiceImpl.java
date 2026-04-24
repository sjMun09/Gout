package com.gout.service.impl;

import com.gout.dao.GuidelineRepository;
import com.gout.dto.response.GuidelineResponse;
import com.gout.entity.Guideline;
import com.gout.global.exception.BusinessException;
import com.gout.global.exception.ErrorCode;
import com.gout.service.GuidelineService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GuidelineServiceImpl implements GuidelineService {

    private final GuidelineRepository guidelineRepository;

    @Override
    @Transactional(readOnly = true)
    public List<GuidelineResponse> getGuidelines(String category, String type) {
        Guideline.GuidelineCategory categoryEnum = parseCategory(category);
        Guideline.GuidelineType typeEnum = parseType(type);

        // PostgreSQL JDBC 드라이버가 JPQL 의 :enum IS NULL 비교를 bytea 로 추론해 기동 시 500 이 떨어지는
        // 이슈가 있어(FoodRepository 에서 먼저 만남), 파생 쿼리로 분기 처리한다.
        List<Guideline> result;
        if (categoryEnum != null && typeEnum != null) {
            result = guidelineRepository.findByIsPublishedTrueAndCategoryAndTypeOrderByCreatedAtDesc(
                    categoryEnum, typeEnum);
        } else if (categoryEnum != null) {
            result = guidelineRepository.findByIsPublishedTrueAndCategoryOrderByCreatedAtDesc(
                    categoryEnum);
        } else if (typeEnum != null) {
            result = guidelineRepository.findByIsPublishedTrueAndTypeOrderByCreatedAtDesc(typeEnum);
        } else {
            result = guidelineRepository.findByIsPublishedTrueOrderByCreatedAtDesc();
        }
        return result.stream()
                .map(GuidelineResponse::of)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public GuidelineResponse findById(String id) {
        Guideline guideline = guidelineRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.GUIDELINE_NOT_FOUND));
        return GuidelineResponse.of(guideline);
    }

    private Guideline.GuidelineCategory parseCategory(String category) {
        if (category == null || category.isBlank()) {
            return null;
        }
        try {
            return Guideline.GuidelineCategory.valueOf(category.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "유효하지 않은 category: " + category);
        }
    }

    private Guideline.GuidelineType parseType(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        try {
            return Guideline.GuidelineType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "유효하지 않은 type: " + type);
        }
    }
}

package com.gout.service.impl;

import com.gout.dao.AgeGroupContentRepository;
import com.gout.dto.response.AgeGroupContentResponse;
import com.gout.entity.AgeGroupContent;
import com.gout.global.exception.BusinessException;
import com.gout.global.exception.ErrorCode;
import com.gout.service.AgeContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AgeContentServiceImpl implements AgeContentService {

    private final AgeGroupContentRepository ageGroupContentRepository;

    @Override
    @Transactional(readOnly = true)
    public AgeGroupContentResponse getByAgeGroup(String ageGroup) {
        AgeGroupContent.AgeGroup enumValue = parseAgeGroup(ageGroup);
        AgeGroupContent content = ageGroupContentRepository.findByAgeGroup(enumValue)
                .orElseThrow(() -> new BusinessException(ErrorCode.AGE_CONTENT_NOT_FOUND));
        return AgeGroupContentResponse.of(content);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgeGroupContentResponse> getAll() {
        return ageGroupContentRepository.findAll().stream()
                .sorted(Comparator.comparing(AgeGroupContent::getAgeGroup))
                .map(AgeGroupContentResponse::of)
                .toList();
    }

    private AgeGroupContent.AgeGroup parseAgeGroup(String ageGroup) {
        if (ageGroup == null || ageGroup.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "ageGroup은 필수입니다.");
        }
        try {
            return AgeGroupContent.AgeGroup.valueOf(ageGroup.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "유효하지 않은 ageGroup: " + ageGroup);
        }
    }
}

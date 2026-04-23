package com.gout.service;

import com.gout.dto.response.AgeGroupContentResponse;

import java.util.List;

public interface AgeContentService {

    AgeGroupContentResponse getByAgeGroup(String ageGroup);

    List<AgeGroupContentResponse> getAll();
}

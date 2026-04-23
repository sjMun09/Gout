package com.gout.service;

import com.gout.dto.response.GuidelineResponse;

import java.util.List;

public interface GuidelineService {

    List<GuidelineResponse> getGuidelines(String category, String type);

    GuidelineResponse findById(String id);
}

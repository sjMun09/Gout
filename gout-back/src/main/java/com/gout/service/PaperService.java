package com.gout.service;

import com.gout.dto.response.PaperResponse;
import org.springframework.data.domain.Page;

public interface PaperService {

    Page<PaperResponse> getPapers(String category, int page, int size);

    PaperResponse findById(String id);
}

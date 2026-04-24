package com.gout.service;

import com.gout.dto.request.CreateReportRequest;
import com.gout.dto.response.ReportResponse;

public interface ReportService {

    ReportResponse create(String reporterId, CreateReportRequest request);
}

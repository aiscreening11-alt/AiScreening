package com.company.recruitment.controller;

import com.company.recruitment.dto.response.ApiResponse;
import com.company.recruitment.dto.response.DashboardMetricsDto;
import com.company.recruitment.service.CandidateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DashboardController {

    private final CandidateService candidateService;

    @GetMapping("/metrics")
    public ResponseEntity<ApiResponse<DashboardMetricsDto>> getMetrics() {
        DashboardMetricsDto metrics = candidateService.getDashboardMetrics();
        return ResponseEntity.ok(ApiResponse.success(metrics, "Dashboard metrics retrieved successfully"));
    }
}

package com.company.recruitment.service;

import com.company.recruitment.dto.request.OverrideStatusRequestDto;
import com.company.recruitment.dto.response.CandidateResponseDto;
import com.company.recruitment.dto.response.CandidateSummaryDto;
import com.company.recruitment.dto.response.DashboardMetricsDto;
import com.company.recruitment.entity.Candidate;
import com.company.recruitment.entity.enums.CandidateStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CandidateService {
    CandidateResponseDto getCandidateById(UUID id);
    Candidate getCandidateEntityById(UUID id);
    Page<CandidateSummaryDto> getCandidates(UUID jobId, CandidateStatus status, Pageable pageable);
    CandidateResponseDto overrideCandidateStatus(UUID id, OverrideStatusRequestDto request);
    DashboardMetricsDto getDashboardMetrics();
    Candidate saveCandidate(Candidate candidate);
}

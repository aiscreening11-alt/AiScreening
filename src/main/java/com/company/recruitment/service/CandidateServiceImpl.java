package com.company.recruitment.service;

import com.company.recruitment.dto.request.OverrideStatusRequestDto;
import com.company.recruitment.dto.response.CandidateResponseDto;
import com.company.recruitment.dto.response.CandidateSummaryDto;
import com.company.recruitment.dto.response.DashboardMetricsDto;
import com.company.recruitment.entity.Candidate;
import com.company.recruitment.entity.enums.CandidateStatus;
import com.company.recruitment.entity.enums.Gender;
import com.company.recruitment.exception.CandidateNotFoundException;
import com.company.recruitment.mapper.CandidateMapper;
import com.company.recruitment.repository.CandidateRepository;
import com.company.recruitment.service.audit.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CandidateServiceImpl implements CandidateService {

    private final CandidateRepository candidateRepository;
    private final CandidateMapper candidateMapper;
    private final AuditLogService auditLogService;

    @Override
    @Transactional(readOnly = true)
    public CandidateResponseDto getCandidateById(UUID id) {
        Candidate candidate = getCandidateEntityById(id);
        return candidateMapper.toResponseDto(candidate);
    }

    @Override
    @Transactional(readOnly = true)
    public Candidate getCandidateEntityById(UUID id) {
        return candidateRepository.findById(id)
                .orElseThrow(() -> new CandidateNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CandidateSummaryDto> getCandidates(UUID jobId, CandidateStatus status, Pageable pageable) {
        Page<Candidate> candidates = candidateRepository.findFiltered(jobId, status, pageable);
        return candidates.map(candidateMapper::toSummaryDto);
    }

    @Override
    @Transactional
    public CandidateResponseDto overrideCandidateStatus(UUID id, OverrideStatusRequestDto request) {
        log.info("Recruiter override for candidate {}: newStatus={}, reason={}", id, request.getNewStatus(), request.getReason());
        Candidate candidate = getCandidateEntityById(id);
        CandidateStatus oldStatus = candidate.getStatus();
        candidate.setStatus(request.getNewStatus());
        Candidate saved = candidateRepository.save(candidate);

        auditLogService.logAction("Candidate", saved.getId().toString(), "OVERRIDE_STATUS",
                request.getPerformedBy() != null ? request.getPerformedBy() : "RECRUITER",
                oldStatus.name(), request.getNewStatus().name(), request.getReason());

        return candidateMapper.toResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardMetricsDto getDashboardMetrics() {
        List<Object[]> counts = candidateRepository.countByStatusGrouped();
        Map<String, Long> breakdown = new HashMap<>();
        long total = 0;

        for (Object[] row : counts) {
            CandidateStatus st = (CandidateStatus) row[0];
            Long count = (Long) row[1];
            if (st != null) {
                breakdown.put(st.name(), count);
            }
            total += count;
        }

        List<Object[]> genderCounts = candidateRepository.countByGenderGrouped();
        Map<String, Long> genderBreakdown = new HashMap<>();
        for (Object[] row : genderCounts) {
            Gender g = (Gender) row[0];
            Long count = (Long) row[1];
            String gName = g != null ? g.name() : Gender.UNSPECIFIED.name();
            genderBreakdown.put(gName, count);
        }

        long totalScheduled = breakdown.getOrDefault(CandidateStatus.INTERVIEW_SCHEDULED.name(), 0L)
                + breakdown.getOrDefault(CandidateStatus.L1_SCHEDULED.name(), 0L)
                + breakdown.getOrDefault(CandidateStatus.L2_SCHEDULED.name(), 0L)
                + breakdown.getOrDefault(CandidateStatus.MANAGERIAL_SCHEDULED.name(), 0L)
                + breakdown.getOrDefault(CandidateStatus.HR_SCHEDULED.name(), 0L);

        return DashboardMetricsDto.builder()
                .totalApplications(total)
                .shortlisted(breakdown.getOrDefault(CandidateStatus.SHORTLISTED.name(), 0L))
                .reviewRequired(breakdown.getOrDefault(CandidateStatus.REVIEW_REQUIRED.name(), 0L))
                .rejected(breakdown.getOrDefault(CandidateStatus.REJECTED.name(), 0L))
                .interviewScheduled(totalScheduled)
                .selected(breakdown.getOrDefault(CandidateStatus.SELECTED.name(), 0L))
                .notSelected(breakdown.getOrDefault(CandidateStatus.NOT_SELECTED.name(), 0L))
                .processingFailed(breakdown.getOrDefault(CandidateStatus.FAILED.name(), 0L))
                .statusBreakdown(breakdown)
                .maleCount(genderBreakdown.getOrDefault(Gender.MALE.name(), 0L))
                .femaleCount(genderBreakdown.getOrDefault(Gender.FEMALE.name(), 0L))
                .otherCount(genderBreakdown.getOrDefault(Gender.OTHER.name(), 0L))
                .unspecifiedCount(genderBreakdown.getOrDefault(Gender.UNSPECIFIED.name(), 0L))
                .genderBreakdown(genderBreakdown)
                .build();
    }

    @Override
    @Transactional
    public Candidate saveCandidate(Candidate candidate) {
        return candidateRepository.save(candidate);
    }
}

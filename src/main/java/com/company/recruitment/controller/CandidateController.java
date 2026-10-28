package com.company.recruitment.controller;

import com.company.recruitment.dto.request.OverrideStatusRequestDto;
import com.company.recruitment.dto.response.ApiResponse;
import com.company.recruitment.dto.response.CandidateResponseDto;
import com.company.recruitment.dto.response.CandidateSummaryDto;
import com.company.recruitment.entity.enums.CandidateStatus;
import com.company.recruitment.service.CandidateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/candidates")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CandidateController {

    private final CandidateService candidateService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<CandidateSummaryDto>>> getCandidates(
            @RequestParam(required = false) UUID jobId,
            @RequestParam(required = false) CandidateStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "appliedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<CandidateSummaryDto> candidates = candidateService.getCandidates(jobId, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(candidates, "Candidates retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CandidateResponseDto>> getCandidateById(@PathVariable UUID id) {
        CandidateResponseDto candidate = candidateService.getCandidateById(id);
        return ResponseEntity.ok(ApiResponse.success(candidate, "Candidate details retrieved successfully"));
    }

    @GetMapping("/job/{jobId}")
    public ResponseEntity<ApiResponse<Page<CandidateSummaryDto>>> getCandidatesByJob(
            @PathVariable UUID jobId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("appliedAt").descending());
        Page<CandidateSummaryDto> candidates = candidateService.getCandidates(jobId, null, pageable);
        return ResponseEntity.ok(ApiResponse.success(candidates, "Job candidates retrieved successfully"));
    }

    @PutMapping("/{id}/override-status")
    public ResponseEntity<ApiResponse<CandidateResponseDto>> overrideStatus(
            @PathVariable UUID id,
            @Valid @RequestBody OverrideStatusRequestDto request) {
        CandidateResponseDto updated = candidateService.overrideCandidateStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Candidate status overridden successfully"));
    }
}

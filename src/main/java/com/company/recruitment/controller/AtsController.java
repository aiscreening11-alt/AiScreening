package com.company.recruitment.controller;

import com.company.recruitment.dto.response.ApiResponse;
import com.company.recruitment.dto.response.AtsResultDto;
import com.company.recruitment.dto.response.CandidateResponseDto;
import com.company.recruitment.entity.Candidate;
import com.company.recruitment.entity.Resume;
import com.company.recruitment.mapper.AtsResultMapper;
import com.company.recruitment.mapper.CandidateMapper;
import com.company.recruitment.service.CandidateService;
import com.company.recruitment.service.gmail.EmailIngestionPipeline;
import com.company.recruitment.service.resume.storage.ResumeStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/candidates")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AtsController {

    private final CandidateService candidateService;
    private final AtsResultMapper atsResultMapper;
    private final CandidateMapper candidateMapper;
    private final ResumeStorageService resumeStorageService;
    private final EmailIngestionPipeline emailIngestionPipeline;

    @GetMapping("/{id}/ats-result")
    public ResponseEntity<ApiResponse<AtsResultDto>> getAtsResult(@PathVariable UUID id) {
        Candidate candidate = candidateService.getCandidateEntityById(id);
        if (candidate.getAtsResult() == null) {
            return ResponseEntity.ok(ApiResponse.success(null, "No ATS evaluation result found for candidate"));
        }
        AtsResultDto dto = atsResultMapper.toDto(candidate.getAtsResult());
        return ResponseEntity.ok(ApiResponse.success(dto, "ATS evaluation result retrieved successfully"));
    }

    @PostMapping("/{id}/rescreen")
    public ResponseEntity<ApiResponse<CandidateResponseDto>> rescreenCandidate(@PathVariable UUID id) {
        Candidate candidate = candidateService.getCandidateEntityById(id);
        Resume resume = candidate.getResume();
        if (resume == null || resume.getStorageKey() == null) {
            return ResponseEntity.badRequest().body(ApiResponse.success(null, "Candidate resume file not available for rescreening"));
        }

        byte[] fileBytes = resumeStorageService.retrieve(resume.getStorageKey());
        Candidate updated = emailIngestionPipeline.processManualResumeUpload(fileBytes, resume.getFileName(), candidate.getJob().getId(), candidate.getSenderEmail());

        return ResponseEntity.ok(ApiResponse.success(candidateMapper.toResponseDto(updated), "Candidate rescreened successfully"));
    }
}

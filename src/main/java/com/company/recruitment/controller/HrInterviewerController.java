package com.company.recruitment.controller;

import com.company.recruitment.dto.request.CreateInterviewerRequestDto;
import com.company.recruitment.dto.request.UpdateInterviewerRequestDto;
import com.company.recruitment.dto.response.ApiResponse;
import com.company.recruitment.dto.response.InterviewerResponseDto;
import com.company.recruitment.service.interview.InterviewerAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/hr/interviewers")
@RequiredArgsConstructor
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class HrInterviewerController {

    private final InterviewerAccountService interviewerAccountService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<InterviewerResponseDto>>> getAllInterviewers() {
        log.info("HR retrieving all interviewer accounts");
        List<InterviewerResponseDto> interviewers = interviewerAccountService.getAllInterviewers();
        return ResponseEntity.ok(ApiResponse.success(interviewers, "All interviewer accounts retrieved successfully"));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<InterviewerResponseDto>>> getActiveInterviewers() {
        log.info("HR retrieving active interviewer accounts");
        List<InterviewerResponseDto> interviewers = interviewerAccountService.getActiveInterviewers();
        return ResponseEntity.ok(ApiResponse.success(interviewers, "Active interviewer accounts retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InterviewerResponseDto>> getInterviewerById(@PathVariable UUID id) {
        log.info("HR retrieving interviewer account by id: {}", id);
        InterviewerResponseDto interviewer = interviewerAccountService.getInterviewerById(id);
        return ResponseEntity.ok(ApiResponse.success(interviewer, "Interviewer account retrieved successfully"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<InterviewerResponseDto>> createInterviewer(
            @Valid @RequestBody CreateInterviewerRequestDto request) {
        log.info("HR creating new interviewer account: {}", request.getEmail());
        InterviewerResponseDto created = interviewerAccountService.createInterviewer(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Interviewer account created successfully. The interviewer can now log in to the Interviewer Portal."));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<InterviewerResponseDto>> updateInterviewer(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateInterviewerRequestDto request) {
        log.info("HR updating interviewer account id: {}", id);
        InterviewerResponseDto updated = interviewerAccountService.updateInterviewer(id, request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Interviewer account updated successfully"));
    }

    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<ApiResponse<Void>> toggleStatus(@PathVariable UUID id) {
        log.info("HR toggling status for interviewer id: {}", id);
        interviewerAccountService.toggleInterviewerStatus(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Interviewer status toggled successfully"));
    }
}

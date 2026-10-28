package com.company.recruitment.controller;

import com.company.recruitment.dto.request.InterviewResultRequestDto;
import com.company.recruitment.dto.request.ScheduleInterviewRequestDto;
import com.company.recruitment.dto.response.ApiResponse;
import com.company.recruitment.dto.response.CandidateResponseDto;
import com.company.recruitment.dto.response.InterviewDto;
import com.company.recruitment.service.interview.InterviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/candidates")
@RequiredArgsConstructor
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class InterviewController {

    private final InterviewService interviewService;

    @PostMapping("/{candidateId}/interviews/schedule")
    public ResponseEntity<ApiResponse<CandidateResponseDto>> scheduleInterview(
            @PathVariable UUID candidateId,
            @Valid @RequestBody ScheduleInterviewRequestDto request) {

        log.info(">>> [CONTROLLER] HR scheduling interview: CandidateId={}, Round={}, Interviewer='{}' <{}>",
                candidateId, request.getRound(), request.getInterviewerName(), request.getInterviewerEmail());
        CandidateResponseDto updated = interviewService.scheduleInterview(candidateId, request);
        return ResponseEntity.ok(ApiResponse.success(updated,
                request.getRound().getDisplayName() + " scheduled successfully! Google Meet invitation sent to candidate & interviewer."));
    }

    @PostMapping("/{candidateId}/interviews/{interviewId}/result")
    public ResponseEntity<ApiResponse<CandidateResponseDto>> submitRoundDecision(
            @PathVariable UUID candidateId,
            @PathVariable UUID interviewId,
            @Valid @RequestBody InterviewResultRequestDto request) {

        CandidateResponseDto updated = interviewService.recordRoundDecision(candidateId, interviewId, request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Interview round decision recorded successfully."));
    }

    @PostMapping("/{candidateId}/interviews/{interviewId}/resend-invitations")
    public ResponseEntity<ApiResponse<String>> resendInterviewInvitations(
            @PathVariable UUID candidateId,
            @PathVariable UUID interviewId) {

        log.info(">>> [CONTROLLER] HR requesting resend interview invitations: CandidateId={}, InterviewId={}", candidateId, interviewId);
        interviewService.resendInterviewInvitation(interviewId);
        return ResponseEntity.ok(ApiResponse.success("Interview invitations resent successfully to candidate and interviewer."));
    }

    @GetMapping("/{candidateId}/interviews")
    public ResponseEntity<ApiResponse<List<InterviewDto>>> getCandidateInterviews(@PathVariable UUID candidateId) {
        List<InterviewDto> interviews = interviewService.getCandidateInterviews(candidateId);
        return ResponseEntity.ok(ApiResponse.success(interviews, "Interview history retrieved successfully."));
    }

    @PostMapping("/{id}/interview-result")
    public ResponseEntity<ApiResponse<CandidateResponseDto>> submitInterviewResult(
            @PathVariable UUID id,
            @Valid @RequestBody InterviewResultRequestDto request) {

        CandidateResponseDto updated = interviewService.recordInterviewResult(id, request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Interview result recorded successfully."));
    }
}

package com.company.recruitment.controller;

import com.company.recruitment.dto.request.InterviewerFeedbackSubmissionDto;
import com.company.recruitment.dto.request.InterviewerLoginRequestDto;
import com.company.recruitment.dto.response.*;
import com.company.recruitment.service.interview.InterviewerAccountService;
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
@RequestMapping("/api/v1/interviewer")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class InterviewerPortalController {

    private final InterviewService interviewService;
    private final InterviewerAccountService interviewerAccountService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<InterviewerAuthResponseDto>> login(
            @Valid @RequestBody InterviewerLoginRequestDto request) {
        log.info("Interviewer login attempt for email: {}", request.getEmail());
        InterviewerAuthResponseDto authResponse = interviewerAccountService.login(request);
        return ResponseEntity.ok(ApiResponse.success(authResponse, "Interviewer authenticated successfully. Welcome to Sanvii Interviewer Portal."));
    }

    @GetMapping("/assigned")
    public ResponseEntity<ApiResponse<List<InterviewerSessionDto>>> getAssignedSessions(@RequestParam String email) {
        log.info("Fetching assigned interview sessions for interviewer email: {}", email);
        List<InterviewerSessionDto> sessions = interviewerAccountService.getAssignedInterviews(email);
        return ResponseEntity.ok(ApiResponse.success(sessions, "Assigned interview sessions retrieved successfully."));
    }

    @GetMapping("/notifications")
    public ResponseEntity<ApiResponse<List<InterviewerNotificationDto>>> getInterviewerNotifications(@RequestParam String email) {
        log.info("Fetching mail notifications for interviewer email: {}", email);
        List<InterviewerNotificationDto> notifications = interviewerAccountService.getInterviewerNotifications(email);
        return ResponseEntity.ok(ApiResponse.success(notifications, "Interviewer mail notifications retrieved successfully."));
    }

    @GetMapping("/session")
    public ResponseEntity<ApiResponse<InterviewerSessionDto>> getSessionByToken(@RequestParam String token) {
        log.info("Fetching interviewer session for token: {}", token);
        InterviewerSessionDto session = interviewService.getInterviewerSessionByToken(token);
        return ResponseEntity.ok(ApiResponse.success(session, "Interview session details retrieved successfully."));
    }

    @GetMapping("/interview/{interviewId}")
    public ResponseEntity<ApiResponse<InterviewerSessionDto>> getSessionById(@PathVariable UUID interviewId) {
        log.info("Fetching interviewer session for interview id: {}", interviewId);
        InterviewerSessionDto session = interviewService.getInterviewerSessionById(interviewId);
        return ResponseEntity.ok(ApiResponse.success(session, "Interview session details retrieved successfully."));
    }

    @PostMapping("/interview/{interviewId}/feedback")
    public ResponseEntity<ApiResponse<CandidateResponseDto>> submitFeedback(
            @PathVariable UUID interviewId,
            @Valid @RequestBody InterviewerFeedbackSubmissionDto request) {
        log.info("Submitting interviewer feedback for interview id={}", interviewId);
        CandidateResponseDto candidate = interviewService.submitInterviewerFeedback(interviewId, request);
        return ResponseEntity.ok(ApiResponse.success(candidate, "Interview feedback and scores submitted successfully! Candidate status updated."));
    }

    @PostMapping("/session/feedback")
    public ResponseEntity<ApiResponse<CandidateResponseDto>> submitFeedbackByToken(
            @RequestParam String token,
            @Valid @RequestBody InterviewerFeedbackSubmissionDto request) {
        log.info("Submitting interviewer feedback using portal token: {}", token);
        CandidateResponseDto candidate = interviewService.submitInterviewerFeedbackByToken(token, request);
        return ResponseEntity.ok(ApiResponse.success(candidate, "Interview feedback and scores submitted successfully! Candidate status updated."));
    }

    @GetMapping("/my-interviews")
    public ResponseEntity<ApiResponse<List<InterviewDto>>> getMyInterviews(@RequestParam String email) {
        log.info("Fetching assigned interviews for interviewer email: {}", email);
        List<InterviewDto> interviews = interviewService.getInterviewsByInterviewerEmail(email);
        return ResponseEntity.ok(ApiResponse.success(interviews, "Assigned interviews retrieved successfully."));
    }

    @GetMapping("/recent-evaluations")
    public ResponseEntity<ApiResponse<List<InterviewDto>>> getRecentEvaluations() {
        List<InterviewDto> evaluations = interviewService.getRecentEvaluations();
        return ResponseEntity.ok(ApiResponse.success(evaluations, "Recent interviewer evaluations retrieved successfully."));
    }
}

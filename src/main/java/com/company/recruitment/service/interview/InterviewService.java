package com.company.recruitment.service.interview;

import com.company.recruitment.dto.request.InterviewResultRequestDto;
import com.company.recruitment.dto.request.InterviewerFeedbackSubmissionDto;
import com.company.recruitment.dto.request.ScheduleInterviewRequestDto;
import com.company.recruitment.dto.response.CandidateResponseDto;
import com.company.recruitment.dto.response.InterviewDto;
import com.company.recruitment.dto.response.InterviewerSessionDto;

import java.util.List;
import java.util.UUID;

public interface InterviewService {
    CandidateResponseDto scheduleInterview(UUID candidateId, ScheduleInterviewRequestDto request);
    CandidateResponseDto recordRoundDecision(UUID candidateId, UUID interviewId, InterviewResultRequestDto request);
    CandidateResponseDto recordInterviewResult(UUID candidateId, InterviewResultRequestDto request);
    List<InterviewDto> getCandidateInterviews(UUID candidateId);

    // Interviewer Portal operations
    InterviewerSessionDto getInterviewerSessionByToken(String accessToken);
    InterviewerSessionDto getInterviewerSessionById(UUID interviewId);
    CandidateResponseDto submitInterviewerFeedback(UUID interviewId, InterviewerFeedbackSubmissionDto feedbackDto);
    CandidateResponseDto submitInterviewerFeedbackByToken(String accessToken, InterviewerFeedbackSubmissionDto feedbackDto);
    List<InterviewDto> getInterviewsByInterviewerEmail(String email);
    List<InterviewDto> getRecentEvaluations();
    void resendInterviewInvitation(UUID interviewId);
}

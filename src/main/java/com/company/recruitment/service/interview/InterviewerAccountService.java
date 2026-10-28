package com.company.recruitment.service.interview;

import com.company.recruitment.dto.request.CreateInterviewerRequestDto;
import com.company.recruitment.dto.request.InterviewerLoginRequestDto;
import com.company.recruitment.dto.request.UpdateInterviewerRequestDto;
import com.company.recruitment.dto.response.InterviewerAuthResponseDto;
import com.company.recruitment.dto.response.InterviewerNotificationDto;
import com.company.recruitment.dto.response.InterviewerResponseDto;
import com.company.recruitment.dto.response.InterviewerSessionDto;

import java.util.List;
import java.util.UUID;

public interface InterviewerAccountService {

    InterviewerResponseDto createInterviewer(CreateInterviewerRequestDto request);

    InterviewerResponseDto updateInterviewer(UUID id, UpdateInterviewerRequestDto request);

    List<InterviewerResponseDto> getAllInterviewers();

    List<InterviewerResponseDto> getActiveInterviewers();

    InterviewerResponseDto getInterviewerById(UUID id);

    void toggleInterviewerStatus(UUID id);

    InterviewerAuthResponseDto login(InterviewerLoginRequestDto request);

    List<InterviewerSessionDto> getAssignedInterviews(String interviewerEmail);

    List<InterviewerNotificationDto> getInterviewerNotifications(String interviewerEmail);
}

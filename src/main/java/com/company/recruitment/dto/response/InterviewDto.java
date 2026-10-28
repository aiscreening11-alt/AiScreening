package com.company.recruitment.dto.response;

import com.company.recruitment.entity.enums.InterviewResult;
import com.company.recruitment.entity.enums.InterviewRound;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewDto {
    private UUID id;
    private InterviewRound round;
    private String roundDisplayName;
    private LocalDateTime interviewDate;
    private String interviewerName;
    private String interviewerEmail;
    private String meetingLink;
    private Integer durationMinutes;
    private String notes;
    private String feedback;
    private Integer technicalRating;
    private Integer communicationRating;
    private Integer confidenceRating;
    private Integer bodyLanguageRating;
    private Double overallRating;
    private String recommendation;
    private String comments;
    private LocalDateTime evaluatedAt;
    private String accessToken;
    private UUID candidateId;
    private String candidateName;
    private String jobTitle;
    private InterviewResult result;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

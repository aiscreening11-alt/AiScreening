package com.company.recruitment.dto.response;

import com.company.recruitment.entity.enums.InterviewResult;
import com.company.recruitment.entity.enums.InterviewRound;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewerSessionDto {
    private UUID interviewId;
    private String accessToken;
    private UUID candidateId;
    private String candidateName;
    private String candidateEmail;
    private String phoneNumber;
    private String jobTitle;
    private String department;
    private Double atsScore;
    private List<String> skills;
    private String resumeFileName;
    private InterviewRound round;
    private String roundDisplayName;
    private LocalDateTime interviewDate;
    private Integer durationMinutes;
    private String meetingLink;
    private String interviewerName;
    private String interviewerEmail;
    private String notes;
    private boolean evaluated;
    private Integer technicalRating;
    private Integer communicationRating;
    private Integer confidenceRating;
    private Integer bodyLanguageRating;
    private Double overallRating;
    private String recommendation;
    private String comments;
    private InterviewResult result;
    private LocalDateTime evaluatedAt;

    public UUID getId() {
        return interviewId;
    }
}

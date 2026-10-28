package com.company.recruitment.dto.response;

import com.company.recruitment.entity.enums.CandidateStatus;
import com.company.recruitment.entity.enums.Gender;
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
public class CandidateResponseDto {
    private UUID id;
    private JobResponseDto job;
    private String fullName;
    private String senderEmail;
    private String resumeEmail;
    private Boolean emailDiscrepancyFlag;
    private String phoneNumber;
    private Double totalExperienceYears;
    private String currentCompany;
    private String previousCompanies;
    private String education;
    private String degree;
    private String location;
    private String noticePeriod;
    private Gender gender;
    private CandidateStatus status;
    private String resumeFileName;
    private String resumeFileType;
    private String extractedResumeText;
    private List<CandidateSkillDto> skills;
    private AtsResultDto atsResult;
    private List<InterviewDto> interviews;
    private List<EmailCommunicationDto> emailCommunications;
    private LocalDateTime appliedAt;
    private LocalDateTime updatedAt;

    public Double getAtsScore() {
        return atsResult != null ? atsResult.getFinalScore() : null;
    }

    public Double getFinalAtsScore() {
        return atsResult != null ? atsResult.getFinalScore() : null;
    }

    public String getEmail() {
        return resumeEmail != null ? resumeEmail : senderEmail;
    }
}

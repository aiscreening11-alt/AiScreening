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
public class CandidateSummaryDto {
    private UUID id;
    private UUID jobId;
    private String jobTitle;
    private String fullName;
    private String email;
    private String phoneNumber;
    private Double totalExperienceYears;
    private String currentCompany;
    private Gender gender;
    private CandidateStatus status;
    private Double finalAtsScore;
    private List<String> topSkills;
    private Boolean emailDiscrepancyFlag;
    private LocalDateTime appliedAt;

    public Double getAtsScore() {
        return finalAtsScore;
    }

    public String getSenderEmail() {
        return email;
    }

    public String getResumeEmail() {
        return email;
    }
}

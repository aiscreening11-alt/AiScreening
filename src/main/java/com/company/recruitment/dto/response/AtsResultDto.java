package com.company.recruitment.dto.response;

import com.company.recruitment.entity.enums.CandidateStatus;
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
public class AtsResultDto {
    private UUID id;
    private Double ruleScore;
    private Double aiScore;
    private Double finalScore;
    private CandidateStatus decision;
    private List<String> matchedSkills;
    private List<String> missingSkills;
    private String scoringBreakdownJson;
    private String aiSummary;
    private Boolean isAiAssisted;
    private LocalDateTime evaluatedAt;
}

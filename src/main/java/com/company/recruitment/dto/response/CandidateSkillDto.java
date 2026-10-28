package com.company.recruitment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateSkillDto {
    private UUID id;
    private String skillName;
    private String normalizedSkill;
    private String category;
    private Boolean isMatchedRequirement;
}

package com.company.recruitment.mapper;

import com.company.recruitment.dto.response.AtsResultDto;
import com.company.recruitment.entity.AtsResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class AtsResultMapper {

    public AtsResultDto toDto(AtsResult entity) {
        if (entity == null) return null;
        return AtsResultDto.builder()
                .id(entity.getId())
                .ruleScore(entity.getRuleScore())
                .aiScore(entity.getAiScore())
                .finalScore(entity.getFinalScore())
                .decision(entity.getDecision())
                .matchedSkills(entity.getMatchedSkills() != null ? new ArrayList<>(entity.getMatchedSkills()) : new ArrayList<>())
                .missingSkills(entity.getMissingSkills() != null ? new ArrayList<>(entity.getMissingSkills()) : new ArrayList<>())
                .scoringBreakdownJson(entity.getScoringBreakdownJson())
                .aiSummary(entity.getAiSummary())
                .isAiAssisted(entity.getIsAiAssisted())
                .evaluatedAt(entity.getEvaluatedAt())
                .build();
    }
}

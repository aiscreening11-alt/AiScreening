package com.company.recruitment.service.ats;

import com.company.recruitment.entity.Job;
import com.company.recruitment.entity.enums.CandidateStatus;
import com.company.recruitment.service.resume.CandidateInfoExtractor;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ATSScoringService {

    @Data
    @Builder
    class RuleBasedEvaluationResult {
        private double totalScore;
        private double skillsScore;
        private double experienceScore;
        private double educationScore;
        private double keywordsScore;
        private double responsibilitiesScore;
        private double certificationsScore;
        private double resumeQualityScore;
        private List<String> matchedSkills;
        private List<String> missingSkills;
        private CandidateStatus screeningDecision;
        private String breakdownJson;
        private Set<String> extractedNormalizedSkills;
    }

    RuleBasedEvaluationResult evaluate(String resumeText, CandidateInfoExtractor.ExtractedCandidateInfo candidateInfo, Job job);
}

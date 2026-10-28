package com.company.recruitment.service.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

public interface AIResumeAnalyzer {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class ResumeAnalysis {
        private String candidateName;
        @Builder.Default
        private List<String> skills = new ArrayList<>();
        private Double totalExperienceYears;
        @Builder.Default
        private List<String> matchedSkills = new ArrayList<>();
        @Builder.Default
        private List<String> missingSkills = new ArrayList<>();
        private Double relevantExperienceScore;
        private Double jobMatchScore;
        private String summary;
    }

    ResumeAnalysis analyzeResume(String resumeText, String jobDescription);
    String getProviderName();
}

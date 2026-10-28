package com.company.recruitment.service.ai;

import com.company.recruitment.service.ats.SkillNormalizationService;
import com.company.recruitment.service.resume.CandidateInfoExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Component("mockAIResumeAnalyzer")
@RequiredArgsConstructor
public class MockAIResumeAnalyzer implements AIResumeAnalyzer {

    private final SkillNormalizationService skillNormalizationService;
    private final CandidateInfoExtractor candidateInfoExtractor;

    @Override
    public ResumeAnalysis analyzeResume(String resumeText, String jobDescription) {
        log.info("Executing MockAIResumeAnalyzer on candidate resume");

        CandidateInfoExtractor.ExtractedCandidateInfo info = candidateInfoExtractor.extract(resumeText, null, null);
        Set<String> skills = skillNormalizationService.extractNormalizedSkillsFromText(resumeText);
        Set<String> jobSkills = skillNormalizationService.extractNormalizedSkillsFromText(jobDescription);

        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();

        for (String js : jobSkills) {
            if (skills.contains(js)) {
                matched.add(js);
            } else {
                missing.add(js);
            }
        }

        double matchRatio = jobSkills.isEmpty() ? 0.8 : (double) matched.size() / jobSkills.size();
        double jobMatchScore = Math.round((matchRatio * 100.0) * 10.0) / 10.0;
        double expScore = Math.min(100.0, (info.getTotalExperienceYears() / 5.0) * 85.0);

        String summary = String.format(
                "Candidate %s has approx %.1f years of experience. Demonstrated skills in %s. Semantic alignment: %.1f%% with target job requirements.",
                info.getFullName(),
                info.getTotalExperienceYears(),
                skills.stream().limit(5).toList(),
                jobMatchScore
        );

        return ResumeAnalysis.builder()
                .candidateName(info.getFullName())
                .skills(new ArrayList<>(skills))
                .totalExperienceYears(info.getTotalExperienceYears())
                .matchedSkills(matched)
                .missingSkills(missing)
                .relevantExperienceScore(Math.round(expScore * 10.0) / 10.0)
                .jobMatchScore(jobMatchScore)
                .summary(summary)
                .build();
    }

    @Override
    public String getProviderName() {
        return "mock";
    }
}

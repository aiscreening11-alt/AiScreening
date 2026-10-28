package com.company.recruitment.service.ats;

import com.company.recruitment.config.AtsScoringProperties;
import com.company.recruitment.entity.Job;
import com.company.recruitment.entity.enums.CandidateStatus;
import com.company.recruitment.service.resume.CandidateInfoExtractor;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ATSScoringServiceImpl implements ATSScoringService {

    private final SkillNormalizationService skillNormalizationService;
    private final AtsScoringProperties atsProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final List<String> CERTIFICATION_KEYWORDS = List.of(
            "certified", "certification", "aws certified", "oracle certified", "cka", "ckad",
            "pmp", "scrum master", "psm", "csm", "spring certified", "azure certified", "gcp certified"
    );

    private static final List<String> RESPONSIBILITY_KEYWORDS = List.of(
            "architect", "designed", "developed", "implemented", "deployed", "scaled",
            "microservices", "leadership", "mentored", "agile", "delivered", "optimized", "built", "maintained"
    );

    @Override
    public RuleBasedEvaluationResult evaluate(String resumeText, CandidateInfoExtractor.ExtractedCandidateInfo candidateInfo, Job job) {
        log.info("Running deterministic ATS evaluation for candidate against job: '{}'", job.getJobTitle());

        AtsScoringProperties.Weights weights = atsProperties.getWeights();

        // 1. Extract candidate skills
        Set<String> extractedSkills = skillNormalizationService.extractNormalizedSkillsFromText(resumeText);

        // 2. Score Skills Match (Max: weights.getSkills() -> 40 points)
        List<String> matchedSkills = new ArrayList<>();
        List<String> missingSkills = new ArrayList<>();
        double skillsScore = calculateSkillsScore(job, extractedSkills, matchedSkills, missingSkills, weights.getSkills());

        // 3. Score Experience Match (Max: weights.getExperience() -> 20 points)
        double experienceScore = calculateExperienceScore(candidateInfo.getTotalExperienceYears(), job.getMinimumExperience(), weights.getExperience());

        // 4. Score Education Match (Max: weights.getEducation() -> 10 points)
        double educationScore = calculateEducationScore(candidateInfo.getDegree(), job.getEducationRequirement(), weights.getEducation());

        // 5. Score Relevant Keywords Match (Max: weights.getKeywords() -> 10 points)
        double keywordsScore = calculateKeywordsScore(resumeText, job.getDescription(), weights.getKeywords());

        // 6. Score Job Responsibilities (Max: weights.getResponsibilities() -> 10 points)
        double responsibilitiesScore = calculateResponsibilitiesScore(resumeText, weights.getResponsibilities());

        // 7. Score Certifications (Max: weights.getCertifications() -> 5 points)
        double certificationsScore = calculateCertificationsScore(resumeText, weights.getCertifications());

        // 8. Score Resume Quality (Max: weights.getResumeQuality() -> 5 points)
        double resumeQualityScore = calculateResumeQualityScore(candidateInfo, resumeText, weights.getResumeQuality());

        // Total score calculation
        double totalScore = skillsScore + experienceScore + educationScore + keywordsScore
                + responsibilitiesScore + certificationsScore + resumeQualityScore;

        totalScore = Math.min(100.0, Math.max(0.0, Math.round(totalScore * 10.0) / 10.0));

        // Decision based on configurable thresholds
        CandidateStatus decision;
        if (totalScore >= atsProperties.getShortlistThreshold()) {
            decision = CandidateStatus.SHORTLISTED;
        } else if (totalScore >= atsProperties.getReviewThreshold()) {
            decision = CandidateStatus.REVIEW_REQUIRED;
        } else {
            decision = CandidateStatus.REJECTED;
        }

        Map<String, Object> breakdown = new LinkedHashMap<>();
        breakdown.put("skillsScore", skillsScore);
        breakdown.put("skillsWeight", weights.getSkills());
        breakdown.put("experienceScore", experienceScore);
        breakdown.put("experienceWeight", weights.getExperience());
        breakdown.put("educationScore", educationScore);
        breakdown.put("educationWeight", weights.getEducation());
        breakdown.put("keywordsScore", keywordsScore);
        breakdown.put("keywordsWeight", weights.getKeywords());
        breakdown.put("responsibilitiesScore", responsibilitiesScore);
        breakdown.put("responsibilitiesWeight", weights.getResponsibilities());
        breakdown.put("certificationsScore", certificationsScore);
        breakdown.put("certificationsWeight", weights.getCertifications());
        breakdown.put("resumeQualityScore", resumeQualityScore);
        breakdown.put("resumeQualityWeight", weights.getResumeQuality());
        breakdown.put("totalScore", totalScore);

        String breakdownJson;
        try {
            breakdownJson = objectMapper.writeValueAsString(breakdown);
        } catch (Exception e) {
            breakdownJson = "{}";
        }

        log.info("ATS evaluation completed. Total rule-based score: {} / 100. Decision: {}", totalScore, decision);

        return RuleBasedEvaluationResult.builder()
                .totalScore(totalScore)
                .skillsScore(skillsScore)
                .experienceScore(experienceScore)
                .educationScore(educationScore)
                .keywordsScore(keywordsScore)
                .responsibilitiesScore(responsibilitiesScore)
                .certificationsScore(certificationsScore)
                .resumeQualityScore(resumeQualityScore)
                .matchedSkills(matchedSkills)
                .missingSkills(missingSkills)
                .screeningDecision(decision)
                .breakdownJson(breakdownJson)
                .extractedNormalizedSkills(extractedSkills)
                .build();
    }

    private double calculateSkillsScore(Job job, Set<String> candidateSkills, List<String> matched, List<String> missing, double maxScore) {
        List<String> required = job.getRequiredSkills() != null ? job.getRequiredSkills() : Collections.emptyList();
        List<String> preferred = job.getPreferredSkills() != null ? job.getPreferredSkills() : Collections.emptyList();

        if (required.isEmpty()) {
            return maxScore;
        }

        int reqMatched = 0;
        for (String req : required) {
            String norm = skillNormalizationService.normalizeSkill(req).getNormalized();
            if (candidateSkills.contains(norm)) {
                matched.add(req);
                reqMatched++;
            } else {
                missing.add(req);
            }
        }

        int prefMatched = 0;
        for (String pref : preferred) {
            String norm = skillNormalizationService.normalizeSkill(pref).getNormalized();
            if (candidateSkills.contains(norm)) {
                matched.add(pref);
                prefMatched++;
            }
        }

        // Required skills make up 80% of skills score, preferred skills make up 20%
        double reqRatio = (double) reqMatched / required.size();
        double prefRatio = preferred.isEmpty() ? 1.0 : (double) prefMatched / preferred.size();

        double score = (reqRatio * 0.80 + prefRatio * 0.20) * maxScore;
        return Math.round(score * 10.0) / 10.0;
    }

    private double calculateExperienceScore(Double candidateExp, Double requiredExp, double maxScore) {
        if (requiredExp == null || requiredExp <= 0.0) {
            return maxScore;
        }
        if (candidateExp == null || candidateExp <= 0.0) {
            return 0.0;
        }
        if (candidateExp >= requiredExp) {
            // Full points plus small bonus if significantly seasoned
            return maxScore;
        }
        // Scaled ratio
        double ratio = candidateExp / requiredExp;
        return Math.round((ratio * maxScore) * 10.0) / 10.0;
    }

    private double calculateEducationScore(String degree, String requirement, double maxScore) {
        if (requirement == null || requirement.isBlank()) {
            return maxScore;
        }
        if (degree == null || degree.isBlank()) {
            return maxScore * 0.5; // Partial points for unmentioned degree
        }
        String lowerDegree = degree.toLowerCase();
        String lowerReq = requirement.toLowerCase();

        if (lowerDegree.contains("ph.d") || lowerDegree.contains("master") || lowerDegree.contains("m.tech") || lowerDegree.contains("m.s")) {
            return maxScore;
        }
        if (lowerDegree.contains("bachelor") || lowerDegree.contains("b.tech") || lowerDegree.contains("b.s") || lowerDegree.contains("b.e")) {
            return maxScore * 0.9;
        }
        if (lowerReq.contains(lowerDegree)) {
            return maxScore;
        }
        return maxScore * 0.7;
    }

    private double calculateKeywordsScore(String resumeText, String jobDesc, double maxScore) {
        if (jobDesc == null || jobDesc.isBlank() || resumeText == null || resumeText.isBlank()) {
            return maxScore * 0.75;
        }
        // Extract salient keywords (>4 chars) from job description
        String[] words = jobDesc.toLowerCase().split("[^a-zA-Z0-9]+");
        Set<String> salient = new HashSet<>();
        for (String w : words) {
            if (w.length() >= 4 && !List.of("with", "have", "will", "this", "that", "from", "they", "must", "work", "team").contains(w)) {
                salient.add(w);
            }
        }
        if (salient.isEmpty()) return maxScore;

        int found = 0;
        String lowerResume = resumeText.toLowerCase();
        for (String kw : salient) {
            if (lowerResume.contains(kw)) {
                found++;
            }
        }
        double ratio = (double) found / Math.min(salient.size(), 15);
        return Math.min(maxScore, Math.round((ratio * maxScore) * 10.0) / 10.0);
    }

    private double calculateResponsibilitiesScore(String resumeText, double maxScore) {
        if (resumeText == null || resumeText.isBlank()) return 0.0;
        String lower = resumeText.toLowerCase();
        int count = 0;
        for (String resp : RESPONSIBILITY_KEYWORDS) {
            if (lower.contains(resp)) count++;
        }
        double ratio = Math.min(1.0, (double) count / 5.0);
        return Math.round((ratio * maxScore) * 10.0) / 10.0;
    }

    private double calculateCertificationsScore(String resumeText, double maxScore) {
        if (resumeText == null || resumeText.isBlank()) return 0.0;
        String lower = resumeText.toLowerCase();
        for (String cert : CERTIFICATION_KEYWORDS) {
            if (lower.contains(cert)) {
                return maxScore;
            }
        }
        return 0.0;
    }

    private double calculateResumeQualityScore(CandidateInfoExtractor.ExtractedCandidateInfo info, String text, double maxScore) {
        int points = 0;
        if (info.getFullName() != null && !info.getFullName().equals("Candidate")) points++;
        if (info.getEmail() != null) points++;
        if (info.getPhoneNumber() != null) points++;
        if (text != null && text.length() > 300) points++;
        if (text != null && (text.contains("•") || text.contains("-") || text.contains("*"))) points++;

        double ratio = (double) points / 5.0;
        return Math.round((ratio * maxScore) * 10.0) / 10.0;
    }
}

package com.company.recruitment.service.ai;

import com.company.recruitment.exception.AIProcessingException;
import com.company.recruitment.util.PiiSanitizer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component("openaiResumeAnalyzer")
public class OpenAiResumeAnalyzer implements AIResumeAnalyzer {

    @Value("${ai.api-key:mock-api-key}")
    private String apiKey;

    @Value("${ai.model:gpt-4o-mini}")
    private String model;

    @Value("${ai.timeout-seconds:15}")
    private int timeoutSeconds;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ResumeAnalysis analyzeResume(String resumeText, String jobDescription) {
        log.info("Invoking OpenAI API ({}) for resume analysis", model);

        if (apiKey == null || apiKey.contains("mock") || apiKey.isBlank()) {
            throw new AIProcessingException("OpenAI API key is not configured");
        }

        String sanitizedResume = PiiSanitizer.sanitize(resumeText);
        String prompt = """
                You are an expert technical recruiter ATS analyzer.
                Analyze this candidate resume against the provided Job Description.
                Return ONLY valid JSON matching this schema:
                {
                  "candidateName": "string",
                  "skills": ["string"],
                  "totalExperienceYears": number,
                  "matchedSkills": ["string"],
                  "missingSkills": ["string"],
                  "relevantExperienceScore": number (0-100),
                  "jobMatchScore": number (0-100),
                  "summary": "string"
                }

                JOB DESCRIPTION:
                %s

                CANDIDATE RESUME (PII Sanitized):
                %s
                """.formatted(jobDescription != null ? jobDescription : "N/A", sanitizedResume);

        try {
            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "response_format", Map.of("type", "json_object"),
                    "messages", List.of(
                            Map.of("role", "system", "content", "You are an AI recruitment ATS analysis assistant. Output strict JSON only."),
                            Map.of("role", "user", "content", prompt)
                    ),
                    "temperature", 0.2
            );

            String requestJson = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new AIProcessingException("OpenAI API returned status " + response.statusCode() + ": " + response.body());
            }

            JsonNode root = objectMapper.readTree(response.body());
            String content = root.path("choices").get(0).path("message").path("content").asText();

            JsonNode parsed = objectMapper.readTree(content);

            List<String> skills = new ArrayList<>();
            if (parsed.has("skills")) {
                parsed.get("skills").forEach(s -> skills.add(s.asText()));
            }

            List<String> matched = new ArrayList<>();
            if (parsed.has("matchedSkills")) {
                parsed.get("matchedSkills").forEach(s -> matched.add(s.asText()));
            }

            List<String> missing = new ArrayList<>();
            if (parsed.has("missingSkills")) {
                parsed.get("missingSkills").forEach(s -> missing.add(s.asText()));
            }

            return ResumeAnalysis.builder()
                    .candidateName(parsed.path("candidateName").asText("Candidate"))
                    .skills(skills)
                    .totalExperienceYears(parsed.path("totalExperienceYears").asDouble(0.0))
                    .matchedSkills(matched)
                    .missingSkills(missing)
                    .relevantExperienceScore(parsed.path("relevantExperienceScore").asDouble(75.0))
                    .jobMatchScore(parsed.path("jobMatchScore").asDouble(75.0))
                    .summary(parsed.path("summary").asText("AI analysis completed."))
                    .build();

        } catch (Exception e) {
            log.error("OpenAI analysis failed: {}", e.getMessage());
            throw new AIProcessingException("Failed to analyze resume with OpenAI: " + e.getMessage(), e);
        }
    }

    @Override
    public String getProviderName() {
        return "openai";
    }
}

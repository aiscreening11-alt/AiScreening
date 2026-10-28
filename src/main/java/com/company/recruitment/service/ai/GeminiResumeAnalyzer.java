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
@Component("geminiResumeAnalyzer")
public class GeminiResumeAnalyzer implements AIResumeAnalyzer {

    @Value("${ai.api-key:mock-api-key}")
    private String apiKey;

    @Value("${ai.model:gemini-1.5-flash}")
    private String model;

    @Value("${ai.timeout-seconds:15}")
    private int timeoutSeconds;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ResumeAnalysis analyzeResume(String resumeText, String jobDescription) {
        log.info("Invoking Google Gemini API ({}) for resume analysis", model);

        if (apiKey == null || apiKey.contains("mock") || apiKey.isBlank()) {
            throw new AIProcessingException("Gemini API key is not configured");
        }

        String sanitizedResume = PiiSanitizer.sanitize(resumeText);
        String prompt = """
                Analyze this candidate resume against the Job Description. Return strictly a JSON object with:
                candidateName, skills (array), totalExperienceYears (number), matchedSkills (array), missingSkills (array), relevantExperienceScore (0-100), jobMatchScore (0-100), summary (string).
                
                JOB:
                %s
                
                RESUME:
                %s
                """.formatted(jobDescription != null ? jobDescription : "N/A", sanitizedResume);

        try {
            String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;

            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(Map.of("text", prompt)))
                    ),
                    "generationConfig", Map.of(
                            "responseMimeType", "application/json"
                    )
            );

            String requestJson = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new AIProcessingException("Gemini API error " + response.statusCode() + ": " + response.body());
            }

            JsonNode root = objectMapper.readTree(response.body());
            String text = root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();

            JsonNode parsed = objectMapper.readTree(text);

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
                    .summary(parsed.path("summary").asText("Gemini analysis completed."))
                    .build();

        } catch (Exception e) {
            log.error("Gemini analysis failed: {}", e.getMessage());
            throw new AIProcessingException("Failed to analyze resume with Gemini: " + e.getMessage(), e);
        }
    }

    @Override
    public String getProviderName() {
        return "gemini";
    }
}

package com.company.recruitment.service.ats;

import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

@Service
public class SkillNormalizationService {

    @Data
    @Builder
    public static class NormalizedSkillInfo {
        private String original;
        private String normalized;
        private String category;
    }

    private static final Map<String, NormalizedSkillInfo> SYNONYM_MAP = new HashMap<>();

    static {
        // Languages
        register("java", "JAVA", "PROGRAMMING_LANGUAGE");
        register("java 8", "JAVA", "PROGRAMMING_LANGUAGE");
        register("java 11", "JAVA", "PROGRAMMING_LANGUAGE");
        register("java 17", "JAVA", "PROGRAMMING_LANGUAGE");
        register("java 21", "JAVA", "PROGRAMMING_LANGUAGE");
        register("core java", "JAVA", "PROGRAMMING_LANGUAGE");
        register("python", "PYTHON", "PROGRAMMING_LANGUAGE");
        register("python3", "PYTHON", "PROGRAMMING_LANGUAGE");
        register("javascript", "JAVASCRIPT", "PROGRAMMING_LANGUAGE");
        register("js", "JAVASCRIPT", "PROGRAMMING_LANGUAGE");
        register("typescript", "TYPESCRIPT", "PROGRAMMING_LANGUAGE");
        register("ts", "TYPESCRIPT", "PROGRAMMING_LANGUAGE");
        register("golang", "GO", "PROGRAMMING_LANGUAGE");
        register("go", "GO", "PROGRAMMING_LANGUAGE");
        register("c++", "C_PLUS_PLUS", "PROGRAMMING_LANGUAGE");
        register("cpp", "C_PLUS_PLUS", "PROGRAMMING_LANGUAGE");
        register("c#", "C_SHARP", "PROGRAMMING_LANGUAGE");
        register("csharp", "C_SHARP", "PROGRAMMING_LANGUAGE");

        // Frameworks & Libraries
        register("spring", "SPRING_BOOT", "FRAMEWORK");
        register("spring boot", "SPRING_BOOT", "FRAMEWORK");
        register("spring boot 3", "SPRING_BOOT", "FRAMEWORK");
        register("spring boot 2", "SPRING_BOOT", "FRAMEWORK");
        register("spring framework", "SPRING_BOOT", "FRAMEWORK");
        register("spring mvc", "SPRING_BOOT", "FRAMEWORK");
        register("spring cloud", "SPRING_BOOT", "FRAMEWORK");
        register("spring data", "SPRING_BOOT", "FRAMEWORK");
        register("spring security", "SPRING_BOOT", "FRAMEWORK");
        register("hibernate", "HIBERNATE", "FRAMEWORK");
        register("jpa", "JPA", "FRAMEWORK");
        register("react", "REACT", "FRAMEWORK");
        register("reactjs", "REACT", "FRAMEWORK");
        register("react.js", "REACT", "FRAMEWORK");
        register("angular", "ANGULAR", "FRAMEWORK");
        register("vue", "VUE", "FRAMEWORK");
        register("node", "NODEJS", "FRAMEWORK");
        register("nodejs", "NODEJS", "FRAMEWORK");
        register("node.js", "NODEJS", "FRAMEWORK");
        register("express", "EXPRESS", "FRAMEWORK");
        register("django", "DJANGO", "FRAMEWORK");
        register("flask", "FLASK", "FRAMEWORK");
        register("fastapi", "FASTAPI", "FRAMEWORK");

        // Architecture & APIs
        register("microservices", "MICROSERVICES", "ARCHITECTURE");
        register("microservice", "MICROSERVICES", "ARCHITECTURE");
        register("microservices architecture", "MICROSERVICES", "ARCHITECTURE");
        register("rest", "REST_API", "ARCHITECTURE");
        register("rest api", "REST_API", "ARCHITECTURE");
        register("restful", "REST_API", "ARCHITECTURE");
        register("restful api", "REST_API", "ARCHITECTURE");
        register("restful apis", "REST_API", "ARCHITECTURE");
        register("graphql", "GRAPHQL", "ARCHITECTURE");
        register("grpc", "GRPC", "ARCHITECTURE");

        // Messaging & Queues
        register("kafka", "KAFKA", "STREAMING");
        register("apache kafka", "KAFKA", "STREAMING");
        register("rabbitmq", "RABBITMQ", "STREAMING");
        register("activemq", "ACTIVEMQ", "STREAMING");
        register("sqs", "AWS_SQS", "STREAMING");

        // Databases
        register("sql", "SQL", "DATABASE");
        register("postgresql", "POSTGRESQL", "DATABASE");
        register("postgres", "POSTGRESQL", "DATABASE");
        register("mysql", "MYSQL", "DATABASE");
        register("oracle", "ORACLE_DB", "DATABASE");
        register("oracle sql", "ORACLE_DB", "DATABASE");
        register("mongodb", "MONGODB", "DATABASE");
        register("mongo", "MONGODB", "DATABASE");
        register("redis", "REDIS", "DATABASE");
        register("elasticsearch", "ELASTICSEARCH", "DATABASE");
        register("dynamodb", "DYNAMODB", "DATABASE");
        register("cassandra", "CASSANDRA", "DATABASE");

        // Cloud & DevOps
        register("aws", "AWS", "CLOUD");
        register("amazon web services", "AWS", "CLOUD");
        register("aws ec2", "AWS", "CLOUD");
        register("aws s3", "AWS", "CLOUD");
        register("aws lambda", "AWS", "CLOUD");
        register("azure", "AZURE", "CLOUD");
        register("microsoft azure", "AZURE", "CLOUD");
        register("gcp", "GCP", "CLOUD");
        register("google cloud", "GCP", "CLOUD");
        register("google cloud platform", "GCP", "CLOUD");
        register("docker", "DOCKER", "DEVOPS");
        register("kubernetes", "KUBERNETES", "DEVOPS");
        register("k8s", "KUBERNETES", "DEVOPS");
        register("terraform", "TERRAFORM", "DEVOPS");
        register("ci/cd", "CICD", "DEVOPS");
        register("cicd", "CICD", "DEVOPS");
        register("jenkins", "JENKINS", "DEVOPS");
        register("git", "GIT", "TOOL");
        register("github", "GIT", "TOOL");
        register("gitlab", "GIT", "TOOL");
        register("maven", "MAVEN", "TOOL");
        register("gradle", "GRADLE", "TOOL");
    }

    private static void register(String alias, String normalized, String category) {
        SYNONYM_MAP.put(alias.toLowerCase().trim(), NormalizedSkillInfo.builder()
                .original(alias)
                .normalized(normalized)
                .category(category)
                .build());
    }

    public NormalizedSkillInfo normalizeSkill(String rawSkill) {
        if (rawSkill == null || rawSkill.trim().isEmpty()) {
            return NormalizedSkillInfo.builder().original("").normalized("UNKNOWN").category("OTHER").build();
        }
        String clean = rawSkill.toLowerCase().trim();
        if (SYNONYM_MAP.containsKey(clean)) {
            return SYNONYM_MAP.get(clean);
        }
        // Fallback: sanitized uppercase token
        String normalized = clean.replaceAll("[^a-zA-Z0-9_+]", "_").toUpperCase();
        return NormalizedSkillInfo.builder()
                .original(rawSkill)
                .normalized(normalized)
                .category("OTHER")
                .build();
    }

    public Set<String> extractNormalizedSkillsFromText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> matchedNormalized = new HashSet<>();
        String lowerText = " " + text.toLowerCase().replaceAll("[,;:/()\\n\\r\\t]", " ") + " ";

        for (Map.Entry<String, NormalizedSkillInfo> entry : SYNONYM_MAP.entrySet()) {
            String alias = entry.getKey();
            // Word boundary regex
            String regex = "(?i)(?<=^|[^a-zA-Z0-9#+])" + Pattern.quote(alias) + "(?=[^a-zA-Z0-9#+]|$)";
            if (Pattern.compile(regex).matcher(lowerText).find()) {
                matchedNormalized.add(entry.getValue().getNormalized());
            }
        }
        return matchedNormalized;
    }
}

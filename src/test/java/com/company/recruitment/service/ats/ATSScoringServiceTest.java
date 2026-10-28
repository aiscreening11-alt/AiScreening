package com.company.recruitment.service.ats;

import com.company.recruitment.config.AtsScoringProperties;
import com.company.recruitment.entity.Job;
import com.company.recruitment.entity.enums.CandidateStatus;
import com.company.recruitment.service.resume.CandidateInfoExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ATSScoringServiceTest {

    private SkillNormalizationService skillNormalizationService;
    private AtsScoringProperties atsProperties;
    private ATSScoringServiceImpl atsScoringService;

    @BeforeEach
    void setUp() {
        skillNormalizationService = new SkillNormalizationService();
        atsProperties = new AtsScoringProperties();
        atsScoringService = new ATSScoringServiceImpl(skillNormalizationService, atsProperties);
    }

    @Test
    void testSkillNormalization() {
        assertEquals("JAVA", skillNormalizationService.normalizeSkill("Java 17").getNormalized());
        assertEquals("JAVA", skillNormalizationService.normalizeSkill("Java 21").getNormalized());
        assertEquals("SPRING_BOOT", skillNormalizationService.normalizeSkill("Spring Cloud").getNormalized());
        assertEquals("SPRING_BOOT", skillNormalizationService.normalizeSkill("Spring Boot 3").getNormalized());
        assertEquals("AWS", skillNormalizationService.normalizeSkill("AWS EC2").getNormalized());
        assertEquals("KAFKA", skillNormalizationService.normalizeSkill("Apache Kafka").getNormalized());
    }

    @Test
    void testEvaluate_ShortlistedCandidate() {
        Job job = Job.builder()
                .jobTitle("Java Spring Boot Developer")
                .description("Build resilient microservices with Java, Spring Boot, Kafka, AWS, and SQL.")
                .requiredSkills(List.of("Java", "Spring Boot", "Microservices", "Kafka", "SQL", "AWS"))
                .preferredSkills(List.of("Docker", "Kubernetes"))
                .minimumExperience(5.0)
                .educationRequirement("B.Tech in Computer Science")
                .build();

        String resumeText = """
                John Doe
                Senior Software Engineer
                Email: john.doe@example.com | Phone: +1-555-0199
                
                SUMMARY:
                Experienced backend engineer with 6.5 years developing enterprise microservices using Java 17, Spring Boot, Spring Cloud, Kafka, PostgreSQL, and AWS EC2/S3.
                
                SKILLS:
                - Java 17, Java 21, Spring Boot, REST API, Microservices
                - Kafka, PostgreSQL, SQL, Docker, AWS
                - Architecture, Agile, CI/CD, Git
                
                EXPERIENCE:
                Senior Engineer at CloudTech (2021 - Present)
                - Architected and deployed microservices handling 10M requests/day.
                - Designed and scaled Kafka event streams.
                - AWS Certified Solutions Architect.
                
                EDUCATION:
                - B.Tech in Computer Science and Engineering
                """;

        CandidateInfoExtractor.ExtractedCandidateInfo info = CandidateInfoExtractor.ExtractedCandidateInfo.builder()
                .fullName("John Doe")
                .email("john.doe@example.com")
                .phoneNumber("+1-555-0199")
                .totalExperienceYears(6.5)
                .degree("B.Tech")
                .currentCompany("CloudTech")
                .build();

        ATSScoringService.RuleBasedEvaluationResult result = atsScoringService.evaluate(resumeText, info, job);

        assertNotNull(result);
        assertTrue(result.getTotalScore() >= 80.0, "Score should be >= 80, but was: " + result.getTotalScore());
        assertEquals(CandidateStatus.SHORTLISTED, result.getScreeningDecision());
        assertTrue(result.getMatchedSkills().contains("Java"));
        assertTrue(result.getMatchedSkills().contains("Spring Boot"));
        assertTrue(result.getMatchedSkills().contains("Kafka"));
        assertTrue(result.getMatchedSkills().contains("AWS"));
    }

    @Test
    void testEvaluate_RejectedCandidate() {
        Job job = Job.builder()
                .jobTitle("Java Spring Boot Developer")
                .description("Build microservices backend")
                .requiredSkills(List.of("Java", "Spring Boot", "Kafka", "AWS", "SQL"))
                .minimumExperience(5.0)
                .build();

        String resumeText = """
                Alice Smith
                Junior Graphic Designer
                Email: alice@example.com
                
                SKILLS:
                - Photoshop, Illustrator, Figma, HTML
                
                EXPERIENCE:
                Graphic Designer (1 year)
                """;

        CandidateInfoExtractor.ExtractedCandidateInfo info = CandidateInfoExtractor.ExtractedCandidateInfo.builder()
                .fullName("Alice Smith")
                .email("alice@example.com")
                .totalExperienceYears(1.0)
                .build();

        ATSScoringService.RuleBasedEvaluationResult result = atsScoringService.evaluate(resumeText, info, job);

        assertNotNull(result);
        assertTrue(result.getTotalScore() < 60.0, "Score should be < 60, but was: " + result.getTotalScore());
        assertEquals(CandidateStatus.REJECTED, result.getScreeningDecision());
    }
}

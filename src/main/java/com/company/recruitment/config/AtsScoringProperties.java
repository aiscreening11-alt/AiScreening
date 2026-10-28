package com.company.recruitment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "ats")
public class AtsScoringProperties {

    private Double shortlistThreshold = 80.0;
    private Double reviewThreshold = 60.0;
    private Weights weights = new Weights();
    private ScoringWeights scoringWeights = new ScoringWeights();

    @Data
    public static class Weights {
        private Double skills = 40.0;
        private Double experience = 20.0;
        private Double education = 10.0;
        private Double keywords = 10.0;
        private Double responsibilities = 10.0;
        private Double certifications = 5.0;
        private Double resumeQuality = 5.0;
    }

    @Data
    public static class ScoringWeights {
        private Double ruleBasedWeight = 0.70;
        private Double aiWeight = 0.30;
    }
}

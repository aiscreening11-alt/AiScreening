package com.company.recruitment.service.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class AIResumeAnalyzerFactory {

    private final Map<String, AIResumeAnalyzer> analyzers;
    private final String defaultProvider;
    private final boolean aiEnabled;

    public AIResumeAnalyzerFactory(
            List<AIResumeAnalyzer> analyzerList,
            @Value("${ai.provider:mock}") String defaultProvider,
            @Value("${ai.enabled:true}") boolean aiEnabled) {
        this.analyzers = analyzerList.stream().collect(Collectors.toMap(AIResumeAnalyzer::getProviderName, a -> a));
        this.defaultProvider = defaultProvider.toLowerCase();
        this.aiEnabled = aiEnabled;
        log.info("Initialized AIResumeAnalyzerFactory with defaultProvider='{}', aiEnabled={}. Available providers: {}",
                this.defaultProvider, this.aiEnabled, analyzers.keySet());
    }

    public boolean isAiEnabled() {
        return aiEnabled;
    }

    public AIResumeAnalyzer getAnalyzer() {
        return getAnalyzer(defaultProvider);
    }

    public AIResumeAnalyzer getAnalyzer(String provider) {
        if (!aiEnabled) {
            return null;
        }
        AIResumeAnalyzer analyzer = analyzers.get(provider.toLowerCase());
        if (analyzer == null) {
            log.warn("AI provider '{}' not found. Falling back to 'mock'", provider);
            return analyzers.get("mock");
        }
        return analyzer;
    }
}

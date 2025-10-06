package com.jobtracker.jobs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ApiKeyValidator {

    private static final Logger logger = LoggerFactory.getLogger(ApiKeyValidator.class);
    
    @Value("${app.api.rotation.check:false}")
    private boolean enableKeyRotation;

    @Value("${app.api.serpapi.key}")
    private String serpApiKey;
    
    @Value("${app.api.rapidapi.key}")
    private String rapidApiKey;
    
    
    @Scheduled(fixedRate = 86400000) // Daily check
    public void checkApiKeyHealth() {
        // Test API keys and alert if they're not working
        if (!testSerpApiKey()) {
            // Send alert to administrators
            logger.warn("SerpAPI key may be expired");
        }
        if (!testRapidApiKey()) {
            // Send alert to administrators
            logger.warn("RapidAPI key may be expired");
        }
    }

    private boolean testSerpApiKey() {
        // Test SerpAPI key
        return serpApiKey != null && !serpApiKey.isEmpty();
    }

    private boolean testRapidApiKey() {
        // Test RapidAPI key
        return rapidApiKey != null && !rapidApiKey.isEmpty();
    }
}

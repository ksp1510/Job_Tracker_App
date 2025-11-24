package com.jobtracker.jobs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ApiKeyValidator {

    private static final Logger logger = LoggerFactory.getLogger(ApiKeyValidator.class);

    // OPTIONAL flag – defaults to false if not found
    @Value("${jobs.keyRotationCheck:false}")
    private boolean enableKeyRotation;

    // NEW: Uses your YAML structure (jobs.rapidapiKey)
    // Also safely defaults to empty "" if missing so app does NOT crash
    @Value("${jobs.rapidapiKey:NOT_SET}")
    private String rapidApiKey;

    @Scheduled(fixedRate = 86400000) // Daily check
    public void checkApiKeyHealth() {
        if (!enableKeyRotation) {
            return;
        }

        if (!testRapidApiKey()) {
            logger.warn("RapidAPI key may be expired or missing.");
        }
    }

    private boolean testRapidApiKey() {
        return rapidApiKey != null && !rapidApiKey.isEmpty();
    }
}

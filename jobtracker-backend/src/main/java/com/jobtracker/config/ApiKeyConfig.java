package com.jobtracker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

import org.springframework.boot.context.properties.ConfigurationProperties;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;


@Configuration
@ConfigurationProperties(prefix = "app.api")
@Data
@Slf4j
public class ApiKeyConfig {
    
    private SerpApi serpapi = new SerpApi();
    private RapidApi rapidapi = new RapidApi();
    private TheirStack theirstack = new TheirStack();
    
    @Data
    public static class SerpApi {
        private String key;
    }
    
    @Data
    public static class RapidApi {
        private String key;
    }
    
    @Data
    public static class TheirStack {
        private String key;
    }
    
    @PostConstruct
    public void validateKeys() {
        if (serpapi.getKey() == null || serpapi.getKey().trim().isEmpty()) {
            log.warn("SerpAPI key is not configured");
        }
        
        if  (rapidapi.getKey() == null || rapidapi.getKey().trim().isEmpty()) {
            log.warn("RapidAPI key is not configured");
        }

        if (theirstack.getKey() == null || theirstack.getKey().trim().isEmpty()) {
            log.warn("THeirStackAPI key is not configured");
        }
    }
}
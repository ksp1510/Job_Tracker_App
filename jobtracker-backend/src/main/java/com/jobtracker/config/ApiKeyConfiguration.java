package com.jobtracker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import lombok.Data;


@Configuration
@EnableConfigurationProperties
public class ApiKeyConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "jobs")
    public ApiKeys apiKeys() {
        return new ApiKeys();
    }

    @Data
    public static class ApiKeys {
        private String rapidApiKey = System.getenv("RAPIDAPI_KEY");
        
        
        @PostConstruct
        public void logConfiguration() {
            System.out.println("🔧 API Keys Configuration:");
            System.out.println("   RapidAPI: " + (rapidApiKey != null && !rapidApiKey.isEmpty() ? "CONFIGURED" : "NOT_CONFIGURED"));
        }
    }
}
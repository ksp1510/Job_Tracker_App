package com.jobtracker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;


@Configuration
@EnableConfigurationProperties
public class ApiKeyConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "app.api")
    public ApiKeys apiKeys() {
        return new ApiKeys();
    }

    @Data
    public static class ApiKeys {
        private String serpApiKey = System.getenv("SERPAPI_KEY");
        private String rapidApiKey = System.getenv("RAPIDAPI_KEY");
        private String theirStackKey = System.getenv("THEIRSTACK_KEY");
        
        @PostConstruct
        public void logConfiguration() {
            System.out.println("🔧 API Keys Configuration:");
            System.out.println("   SerpAPI: " + (serpApiKey != null && !serpApiKey.isEmpty() ? "CONFIGURED" : "NOT_CONFIGURED"));
            System.out.println("   RapidAPI: " + (rapidApiKey != null && !rapidApiKey.isEmpty() ? "CONFIGURED" : "NOT_CONFIGURED"));
            System.out.println("   TheirStack: " + (theirStackKey != null && !theirStackKey.isEmpty() ? "CONFIGURED" : "NOT_CONFIGURED"));
        }
    }
}
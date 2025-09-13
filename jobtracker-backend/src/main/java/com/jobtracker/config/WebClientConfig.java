package com.jobtracker.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration class for WebClient beans used to call external services.
 *
 * The AI microservice URL is configured via the ``ai.service.url`` property
 * defined in ``application.properties``.  This bean can be injected
 * wherever a ``WebClient`` is required.
 */
@Configuration
public class WebClientConfig {

    @Value("${ai.service.url}")
    private String aiServiceUrl;

    /**
     * WebClient configured with the base URL of the AI microservice.
     *
     * @return a new {@link WebClient} instance
     */
    @Bean
    public WebClient aiWebClient() {
        return WebClient.builder()
                .baseUrl(aiServiceUrl)
                .build();
    }
}
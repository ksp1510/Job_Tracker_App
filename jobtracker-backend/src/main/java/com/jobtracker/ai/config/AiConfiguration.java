// AI Configuration
package com.jobtracker.ai.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "app.ai.enabled", havingValue = "true")
public class AiConfiguration {
    
    // AI-related beans and configurations go here
    // For example: OpenAI client, model configurations, etc.
    
    /*
    @Bean
    public OpenAiService openAiService() {
        return new OpenAiService("your-api-key");
    }
    */
}
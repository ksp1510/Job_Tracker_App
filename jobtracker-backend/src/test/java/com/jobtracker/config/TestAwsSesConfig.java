package com.jobtracker.config;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.services.ses.SesClient;

@TestConfiguration
public class TestAwsSesConfig {
    @Bean
    public SesClient sesClient() {
        return Mockito.mock(SesClient.class);
    }
}
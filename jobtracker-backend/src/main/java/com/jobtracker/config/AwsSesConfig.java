package com.jobtracker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;

@Configuration
public class AwsSesConfig {

    @Value("${AWS_REGION:us-east-1}")
    private String region;


    @Value("${AWS_SES_FROM_EMAIL:}")
    private String fromEmail;

    @PostConstruct
    public void logSesConfiguration() {
        System.out.println("📧 ========== AWS SES CONFIGURATION ==========");
        System.out.println("📧 Region: " + region);
        System.out.println("📧 From Email: " + (fromEmail != null && !fromEmail.isEmpty() ? fromEmail : "NOT CONFIGURED"));
        System.out.println("📧 AWS_ACCESS_KEY_ID: " + (System.getenv("AWS_ACCESS_KEY_ID") != null ? "SET" : "NOT SET"));
        System.out.println("📧 AWS_SECRET_ACCESS_KEY: " + (System.getenv("AWS_SECRET_ACCESS_KEY") != null ? "SET" : "NOT SET"));
        System.out.println("📧 ============================================");
    }

    @Bean
    public SesClient sesClient() {
        Region awsRegion = Region.of(region);
        System.out.println("📧 Creating SES Client for region: " + awsRegion);
        
        return SesClient.builder()
                .region(awsRegion)
                .build();
    }
}

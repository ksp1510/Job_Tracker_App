package com.jobtracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import jakarta.annotation.PostConstruct;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Autowired;

@SpringBootApplication(scanBasePackages = "com.jobtracker")
@EnableScheduling
public class JobtrackerBackendApplication {

    @Autowired
    private Environment env;

	public static void main(String[] args) {
		SpringApplication.run(JobtrackerBackendApplication.class, args);
		System.out.println("🚀 JobTracker Backend Started!");
		System.out.println("🔧 Notification system is active");
	}

    @PostConstruct
    public void printConfig() {
        System.out.println("==== ENV CHECK ====");
        
        // Safely get environment variables
        String mongoUri = System.getenv("MONGODB_URI");
        String awsRegion = System.getenv("AWS_REGION");
        
        System.out.println("Mongo URI (env) = " + (mongoUri != null ? "***set***" : "not set"));
        System.out.println("AWS Region (env) = " + (awsRegion != null ? awsRegion : "not set"));
        
        // Get from YAML (these will use defaults if env vars not set)
        String yamlMongoUri = env.getProperty("spring.data.mongodb.uri");
        String yamlAwsRegion = env.getProperty("aws.region");
        
        System.out.println("Mongo URI (yaml) = " + (yamlMongoUri != null ? "***configured***" : "not configured"));
        System.out.println("AWS Region (yaml) = " + (yamlAwsRegion != null ? yamlAwsRegion : "not configured"));
        
        // Print active profile
        String[] activeProfiles = env.getActiveProfiles();
        if (activeProfiles.length > 0) {
            System.out.println("Active Profiles = " + String.join(", ", activeProfiles));
        } else {
            System.out.println("Active Profiles = none (using defaults)");
        }
        
        System.out.println("====================");
    }
}
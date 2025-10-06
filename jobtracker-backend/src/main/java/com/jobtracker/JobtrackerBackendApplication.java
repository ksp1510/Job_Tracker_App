
package com.jobtracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import jakarta.annotation.PostConstruct;

@SpringBootApplication(scanBasePackages = "com.jobtracker")
@EnableScheduling
public class JobtrackerBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(JobtrackerBackendApplication.class, args);
		System.out.println("🚀 JobTracker Backend Started!");
		System.out.println("📧 Notification system is active");
	}

	/*@Bean
	CommandLineRunner runner(UserRepository userRepository) {
    return args -> {
        User user = new User();
        user.setEmail("test@example.com");
        user.setPasswordHash("dummy123");
        user.setRole("USER");
        userRepository.save(user);

        System.out.println("✅ Dummy user inserted!");
    };
}*/
    /*@Bean
    @ConditionalOnProperty(name = "app.jobs.seed.enabled", havingValue = "true")
    CommandLineRunner seedJobs(JobDataSeedService seedService) {
        return args -> {
            // Seed will run through scheduled job instead
            System.out.println("🌱 Job seeding enabled");
        };
    }*/

    @PostConstruct
    public void printMongoUri() {
        System.out.println("MONGODB_URI = " + System.getenv("MONGODB_URI"));
        System.out.println("MONGODB_DB = " + System.getenv("MONGODB_DB"));
        System.out.println("JWT_SECRET = " + System.getenv("JWT_SECRET"));
        System.out.println("AWS_ACCESS_KEY_ID = " + System.getenv("AWS_ACCESS_KEY_ID"));
        System.out.println("AWS_SECRET_ACCESS_KEY = " + System.getenv("AWS_SECRET_ACCESS_KEY"));
        System.out.println("AWS_REGION = " + System.getenv("AWS_REGION"));
        System.out.println("AWS_S3_BUCKET = " + System.getenv("AWS_S3_BUCKET"));
        System.out.println("AWS_SES_FROM_EMAIL = " + System.getenv("AWS_SES_FROM_EMAIL"));
        System.out.println("AWS_SES_TEST_EMAIL = " + System.getenv("AWS_SES_TEST_EMAIL"));
        System.out.println("AWS_SES_REGION = " + System.getenv("AWS_SES_REGION"));
        System.out.println("AWS_SES_ACCESS_KEY_ID = " + System.getenv("AWS_SES_ACCESS_KEY_ID"));
        System.out.println("AWS_SES_SECRET_ACCESS_KEY = " + System.getenv("AWS_SES_SECRET_ACCESS_KEY"));
        System.out.println("SERPAPI_KEY = " + System.getenv("SERPAPI_KEY"));
        System.out.println("RAPIDAPI_KEY = " + System.getenv("RAPIDAPI_KEY"));
    }

}

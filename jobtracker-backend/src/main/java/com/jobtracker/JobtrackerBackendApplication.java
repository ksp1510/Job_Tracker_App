
package com.jobtracker;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.jobtracker.service.JobDataSeedService;

@SpringBootApplication
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
    @Bean
    @ConditionalOnProperty(name = "app.jobs.seed.enabled", havingValue = "true")
    CommandLineRunner seedJobs(JobDataSeedService seedService) {
        return args -> {
            // Seed will run through scheduled job instead
            System.out.println("🌱 Job seeding enabled");
        };
    }

}

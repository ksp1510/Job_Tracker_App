
package com.jobtracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class JobtrackerBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(JobtrackerBackendApplication.class, args);
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


}

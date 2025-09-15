// Job Data Seeding Service (for development/testing)
package com.jobtracker.service;

import com.jobtracker.model.JobListing;
import com.jobtracker.repository.JobListingRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Service
public class JobDataSeedService {

    private final JobListingRepository jobListingRepository;
    private final Random random = new Random();

    public JobDataSeedService(JobListingRepository jobListingRepository) {
        this.jobListingRepository = jobListingRepository;
    }

    /**
     * Seed sample job data for testing
     */
    public void seedSampleJobs() {
        if (jobListingRepository.count() > 0) {
            System.out.println("🌱 Job listings already exist, skipping seed");
            return;
        }

        List<JobListing> sampleJobs = createSampleJobs();
        jobListingRepository.saveAll(sampleJobs);
        System.out.println("🌱 Seeded " + sampleJobs.size() + " sample job listings");
    }

    private List<JobListing> createSampleJobs() {
        return Arrays.asList(
                createJob("Software Engineer", "Google", "Mountain View, CA", 
                         "Build scalable systems", "FULL_TIME", "MID", 120000.0,
                         Arrays.asList("Java", "Python", "Kubernetes"), "https://google.com/jobs/1"),
                
                createJob("Frontend Developer", "Meta", "Menlo Park, CA",
                         "Create beautiful user interfaces", "FULL_TIME", "SENIOR", 150000.0,
                         Arrays.asList("React", "TypeScript", "CSS"), "https://meta.com/jobs/2"),
                
                createJob("Backend Engineer", "Netflix", "Los Gatos, CA",
                         "Scale streaming infrastructure", "FULL_TIME", "SENIOR", 180000.0,
                         Arrays.asList("Java", "Microservices", "AWS"), "https://netflix.com/jobs/3"),
                
                createJob("Data Scientist", "Uber", "San Francisco, CA",
                         "Analyze ride data and optimize algorithms", "FULL_TIME", "MID", 140000.0,
                         Arrays.asList("Python", "SQL", "Machine Learning"), "https://uber.com/jobs/4"),
                
                createJob("DevOps Engineer", "Amazon", "Seattle, WA",
                         "Manage cloud infrastructure", "FULL_TIME", "MID", 130000.0,
                         Arrays.asList("AWS", "Docker", "Terraform"), "https://amazon.com/jobs/5"),
                
                createJob("Mobile Developer", "Spotify", "New York, NY",
                         "Build music streaming apps", "FULL_TIME", "MID", 125000.0,
                         Arrays.asList("React Native", "iOS", "Android"), "https://spotify.com/jobs/6"),
                
                createJob("Full Stack Developer", "Airbnb", "Remote",
                         "End-to-end feature development", "FULL_TIME", "MID", 135000.0,
                         Arrays.asList("React", "Node.js", "PostgreSQL"), "https://airbnb.com/jobs/7"),
                
                createJob("Machine Learning Engineer", "OpenAI", "San Francisco, CA",
                         "Train and deploy AI models", "FULL_TIME", "SENIOR", 200000.0,
                         Arrays.asList("Python", "TensorFlow", "PyTorch"), "https://openai.com/jobs/8"),
                
                createJob("Software Engineer Intern", "Microsoft", "Redmond, WA",
                         "3-month internship program", "INTERNSHIP", "ENTRY", 50000.0,
                         Arrays.asList("C#", ".NET", "Azure"), "https://microsoft.com/jobs/9"),
                
                createJob("Technical Lead", "Tesla", "Palo Alto, CA",
                         "Lead autonomous driving software team", "FULL_TIME", "SENIOR", 170000.0,
                         Arrays.asList("C++", "Python", "Computer Vision"), "https://tesla.com/jobs/10")
        );
    }

    private JobListing createJob(String title, String company, String location, String description,
                               String jobType, String experienceLevel, Double salary, 
                               List<String> skills, String applyUrl) {
        JobListing job = new JobListing();
        job.setExternalId("SEED-" + random.nextInt(100000));
        job.setTitle(title);
        job.setCompany(company);
        job.setLocation(location);
        job.setDescription(description);
        job.setJobType(jobType);
        job.setExperienceLevel(experienceLevel);
        job.setSalary(salary);
        job.setSalaryRange("$" + salary.intValue() + "k - $" + (salary.intValue() + 20) + "k");
        job.setSkills(skills);
        job.setApplyUrl(applyUrl);
        job.setSource("SEED_DATA");
        job.setPostedDate(Instant.now().minus(random.nextInt(30), ChronoUnit.DAYS));
        job.setActive(true);
        return job;
    }
}
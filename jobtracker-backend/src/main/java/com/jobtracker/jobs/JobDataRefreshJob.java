// Scheduled Job to refresh external job data
package com.jobtracker.jobs;

import com.jobtracker.service.ExternalJobApiService;
import com.jobtracker.service.JobDataSeedService;
import com.jobtracker.service.JobSearchService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class JobDataRefreshJob {

    private final ExternalJobApiService externalJobApiService;
    private final JobSearchService jobSearchService;
    private final JobDataSeedService seedService;

    public JobDataRefreshJob(ExternalJobApiService externalJobApiService,
                           JobSearchService jobSearchService,
                           JobDataSeedService seedService) {
        this.externalJobApiService = externalJobApiService;
        this.jobSearchService = jobSearchService;
        this.seedService = seedService;
    }

    /**
     * Refresh job data every 6 hours
     */
    @Scheduled(fixedRate = 21600000) // 6 hours
    public void refreshJobData() {
        try {
            System.out.println("🔄 Starting job data refresh...");
            
            // Clean old jobs first
            jobSearchService.cleanOldJobListings();
            
            // Fetch new jobs from external APIs
            // TODO: Add popular search terms/locations
            externalJobApiService.fetchJobsFromAllSources("Software Engineer", "Toronto")
                    .thenRun(() -> System.out.println("✅ Job data refresh completed"));
            
        } catch (Exception e) {
            System.err.println("❌ Job data refresh failed: " + e.getMessage());
        }
    }

    /**
     * One-time seed on startup (development only)
     */
    @Scheduled(fixedDelay = Long.MAX_VALUE, initialDelay = 5000)
    public void seedInitialData() {
        seedService.seedSampleJobs();
    }
}
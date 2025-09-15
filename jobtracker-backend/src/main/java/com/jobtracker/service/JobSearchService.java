// Job Search Service
package com.jobtracker.service;

import com.jobtracker.model.JobListing;
import com.jobtracker.model.JobSearch;
import com.jobtracker.model.SavedJob;
import com.jobtracker.repository.JobListingRepository;
import com.jobtracker.repository.JobSearchRepository;
import com.jobtracker.repository.SavedJobRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class JobSearchService {

    private final JobListingRepository jobListingRepository;
    private final JobSearchRepository jobSearchRepository;
    private final SavedJobRepository savedJobRepository;
    private final WebClient webClient;

    public JobSearchService(JobListingRepository jobListingRepository,
                           JobSearchRepository jobSearchRepository,
                           SavedJobRepository savedJobRepository,
                           WebClient.Builder webClientBuilder) {
        this.jobListingRepository = jobListingRepository;
        this.jobSearchRepository = jobSearchRepository;
        this.savedJobRepository = savedJobRepository;
        this.webClient = webClientBuilder.build();
    }

    /**
     * Search jobs with filters
     */
    public Page<JobListing> searchJobs(String query, String location, String jobType,
                                     Double minSalary, Double maxSalary, List<String> skills,
                                     int page, int size, String userId) {
        
        // Save search history
        saveSearchHistory(userId, query, location, jobType, minSalary, maxSalary, skills);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "postedDate"));
        
        // For now, search in local database
        // TODO: Integrate with external job APIs
        if (query != null && !query.trim().isEmpty()) {
            return jobListingRepository.findByTitleContainingIgnoreCase(query.trim(), pageable);
        } else if (location != null && !location.trim().isEmpty()) {
            return jobListingRepository.findByLocationContainingIgnoreCase(location.trim(), pageable);
        } else {
            return jobListingRepository.findActiveJobs(pageable);
        }
    }

    /**
     * Get job by ID
     */
    public Optional<JobListing> getJobById(String id) {
        return jobListingRepository.findById(id);
    }

    /**
     * Save job for user
     */
    public SavedJob saveJob(String userId, String jobListingId, String notes) {
        // Check if already saved
        if (savedJobRepository.existsByUserIdAndJobListingId(userId, jobListingId)) {
            throw new RuntimeException("Job already saved");
        }
        
        SavedJob savedJob = new SavedJob();
        savedJob.setUserId(userId);
        savedJob.setJobListingId(jobListingId);
        savedJob.setNotes(notes);
        
        return savedJobRepository.save(savedJob);
    }

    /**
     * Get user's saved jobs
     */
    public List<SavedJob> getSavedJobs(String userId) {
        return savedJobRepository.findByUserIdOrderBySavedAtDesc(userId);
    }

    /**
     * Remove saved job
     */
    public void unsaveJob(String userId, String savedJobId) {
        SavedJob savedJob = savedJobRepository.findById(savedJobId)
                .orElseThrow(() -> new RuntimeException("Saved job not found"));
        
        if (!savedJob.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }
        
        savedJobRepository.deleteById(savedJobId);
    }

    /**
     * Get search history for user
     */
    public List<JobSearch> getSearchHistory(String userId) {
        return jobSearchRepository.findTop10ByUserIdOrderBySearchedAtDesc(userId);
    }

    /**
     * Clean old job listings (scheduled task)
     */
    public void cleanOldJobListings() {
        Instant cutoff = Instant.now().minus(30, ChronoUnit.DAYS);
        List<JobListing> oldJobs = jobListingRepository.findByFetchedAtBefore(cutoff);
        
        for (JobListing job : oldJobs) {
            job.setActive(false);
        }
        
        jobListingRepository.saveAll(oldJobs);
        System.out.println("🧹 Cleaned " + oldJobs.size() + " old job listings");
    }

    // Private helper methods
    private void saveSearchHistory(String userId, String query, String location, 
                                 String jobType, Double minSalary, Double maxSalary, 
                                 List<String> skills) {
        JobSearch search = new JobSearch();
        search.setUserId(userId);
        search.setQuery(query);
        search.setLocation(location);
        search.setJobType(jobType);
        search.setMinSalary(minSalary);
        search.setMaxSalary(maxSalary);
        search.setSkills(skills);
        
        jobSearchRepository.save(search);
    }
}
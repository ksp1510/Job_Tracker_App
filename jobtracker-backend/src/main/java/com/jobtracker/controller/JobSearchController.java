package com.jobtracker.controller;

import com.jobtracker.model.JobListing;
import com.jobtracker.model.JobSearch;
import com.jobtracker.model.SavedJob;
import com.jobtracker.util.UserContext;
import com.jobtracker.repository.SavedJobRepository;
import com.jobtracker.service.JobSearchService;
import com.jobtracker.exception.ResourceNotFoundException;
import lombok.Data;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/jobs")
public class JobSearchController {

    private final JobSearchService jobSearchService;
    private final SavedJobRepository savedJobRepository;

    public JobSearchController(JobSearchService jobSearchService, SavedJobRepository savedJobRepository) {
        this.jobSearchService = jobSearchService;
        this.savedJobRepository = savedJobRepository;
    }

    /**
     * Check if user has cached search results
     */
    @GetMapping("/cache/status")
    public ResponseEntity<CacheStatusResponse> checkCacheStatus() {
        String userId = UserContext.getUserId();
        boolean hasCached = jobSearchService.hasCachedResults(userId);
        return ResponseEntity.ok(new CacheStatusResponse(hasCached));
    }

    /**
     * Get cached search results
     */
    @GetMapping("/cache")
    public ResponseEntity<PaginatedJobResponse> getCachedSearch(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String jobType,
            @RequestParam(required = false) Double minSalary,
            @RequestParam(required = false) Double maxSalary,
            @RequestParam(required = false) List<String> skills) {
        String userId = UserContext.getUserId();
        
        Optional<Page<JobListing>> cachedResults = jobSearchService.getCachedSearch(
            userId, page, size, query, location);
        
        if (cachedResults.isPresent()) {
            Page<JobListing> jobs = cachedResults.get();
            
            // FIXED: Remove duplicates based on externalId
            List<JobListing> uniqueJobs = removeDuplicates(jobs.getContent());
            List<JobListing> validJobs = validateJobsExist(uniqueJobs);

            int actualTotal = validJobs.size();
            int actualPages = (int) Math.ceil((double) actualTotal / size);

            System.out.println("📊 Validation: " + uniqueJobs.size() + " unique → " + validJobs.size() + " valid");
            System.out.println("📄 Page " + page + ": Returning " + validJobs.size() + " jobs (Total: " + actualTotal + ")");
            
            PaginatedJobResponse response = new PaginatedJobResponse(
                validJobs,
                actualTotal,
                actualPages,
                page,
                size,
                    "cache"
            );
            return ResponseEntity.ok(response);
        }
        
        return ResponseEntity.noContent().build();
    }

    private List<JobListing> validateJobsExist(List<JobListing> jobs) {
        return jobs.stream()
            .filter(job -> jobSearchService.getJobById(job.getId()).isPresent())
            .collect(Collectors.toList());
    }

    /**
     * Search jobs with filters - FIXED: Remove duplicates
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchJobs(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String jobType,
            @RequestParam(required = false) Double minSalary,
            @RequestParam(required = false) Double maxSalary,
            @RequestParam(required = false) List<String> skills,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        String userId = UserContext.getUserId();
        
        // Get ALL matching jobs (sorted by recent first)
        Page<JobListing> jobs = jobSearchService.searchJobs(
                query, location, jobType, minSalary, maxSalary, skills, 
                page, size, userId);

        // ✅ FIX: Calculate pagination BEFORE deduplication
        long totalJobsFromDB = jobs.getTotalElements();
        List<JobListing> allJobsFromPage = jobs.getContent();
        
        System.out.println("📊 Total jobs from DB: " + totalJobsFromDB);
        System.out.println("📊 Jobs in current page: " + allJobsFromPage.size());
        
        // Remove duplicates from current page only
        List<JobListing> uniquePageJobs = removeDuplicates(allJobsFromPage);
        
        // ✅ Calculate total pages based on DB count (not deduplicated count)
        int totalPages = (int) Math.ceil((double) totalJobsFromDB / size);
        
        System.out.println("📊 Unique jobs after dedup: " + uniquePageJobs.size());
        System.out.println("📊 Total pages: " + totalPages);
        
        PaginatedJobResponse response = new PaginatedJobResponse(
                uniquePageJobs,     // Deduplicated jobs for THIS page
                totalJobsFromDB,    // Total count from DB (before dedup)
                totalPages,         // Correct page count
                page,
                size,
                "server"
        );
        
        return ResponseEntity.ok(response);
    }


    /**
     * Clear cached search results
     */
    @DeleteMapping("/cache")
    public ResponseEntity<Void> clearCache() {
        String userId = UserContext.getUserId();
        jobSearchService.clearCache(userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get job details by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<JobListing> getJob(@PathVariable String id) {
        return jobSearchService.getJobById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
    }

    /**
     * Save/bookmark a job
     */
    @PostMapping("/{id}/save")
    public ResponseEntity<SavedJob> saveJob(
            @PathVariable String id,
            @RequestBody(required = false) SaveJobRequest request) {
        
        String userId = UserContext.getUserId();
        String notes = request != null ? request.getNotes() : null;

        try {
            Optional<SavedJob> existing = savedJobRepository.findByUserIdAndJobListingId(userId, id);
            if (existing.isPresent()) {
                return ResponseEntity.ok(existing.get());
            }
        SavedJob savedJob = jobSearchService.saveJob(userId, id, notes)
            .orElseThrow(() -> new RuntimeException("Failed to save job"));
        
        return ResponseEntity.ok(savedJob);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save job: " + e.getMessage());
        }
    }

    /**
     * Get user's saved jobs
     */
    @GetMapping("/saved")
    public ResponseEntity<List<SavedJob>> getSavedJobs() {
        
        String userId = UserContext.getUserId();
        return ResponseEntity.ok(jobSearchService.getSavedJobs(userId));
    }

    /**
     * Remove saved job
     */
    @DeleteMapping("/saved/{id}")
    public ResponseEntity<Void> unsaveJob(
            @PathVariable String id) {
        
        String userId = UserContext.getUserId();
        jobSearchService.unsaveJob(userId, id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get search history
     */
    @GetMapping("/search-history")
    public ResponseEntity<List<JobSearch>> getSearchHistory() {
        
        String userId = UserContext.getUserId();
        return ResponseEntity.ok(jobSearchService.getSearchHistory(userId));
    }

    // FIXED: Helper method to remove duplicate jobs
    private List<JobListing> removeDuplicates(List<JobListing> jobs) {
        Map<String, JobListing> uniqueMap = new LinkedHashMap<>();

        for (JobListing job : jobs) {
            String key = job.getExternalId() != null ? job.getExternalId() : job.getId();
            
            if (uniqueMap.containsKey(key)) {
                JobListing existingJob = uniqueMap.get(key);
                if (isMoreComplete(job, existingJob)) {
                    uniqueMap.put(key, job);
                }
            } else {
                uniqueMap.put(key, job);
            }
        }

        return new ArrayList<>(uniqueMap.values());
    }

    // ✅ Helper: Score job completeness
    private boolean isMoreComplete(JobListing job1, JobListing job2) {
        return calculateCompletenessScore(job1) > calculateCompletenessScore(job2);
    }

    private int calculateCompletenessScore(JobListing job) {
        int score = 0;
        if (job.getDescription() != null && !job.getDescription().isEmpty()) score += 3;
        if (job.getSalary() != null || job.getSalaryRange() != null) score += 2;
        if (job.getSkills() != null && !job.getSkills().isEmpty()) score += 2;
        if (job.getApplyUrl() != null && !job.getApplyUrl().isEmpty()) score += 1;
        if (job.getExperienceLevel() != null) score += 1;
        return score;
    }

    // DTOs
    @Data
    static class SaveJobRequest {
        private String notes;
    }

    @Data
    static class CacheStatusResponse {
        private final boolean hasCachedResults;
    }

    @Data
    static class PaginatedJobResponse {
        private final List<JobListing> content;
        private final long totalElements;
        private final int totalPages;
        private final int page;
        private final int size;
        private final String mode;  // NEW FIELD
    }

}
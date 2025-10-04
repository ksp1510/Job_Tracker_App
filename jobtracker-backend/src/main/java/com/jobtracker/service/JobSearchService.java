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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class JobSearchService {

    private final JobListingRepository jobListingRepository;
    private final JobSearchRepository jobSearchRepository;
    private final SavedJobRepository savedJobRepository;
    private final ExternalJobApiService externalJobApiService;

    // Cache structure: userId -> SearchCacheEntry
    private final ConcurrentHashMap<String, SearchCacheEntry> searchCache = new ConcurrentHashMap<>();

    // Cache validity duration (1 hour)
    private static final long CACHE_VALIDITY_MINUTES = 60;

    public JobSearchService(JobListingRepository jobListingRepository,
                           JobSearchRepository jobSearchRepository,
                           SavedJobRepository savedJobRepository,
                           ExternalJobApiService externalJobApiService) {
        this.jobListingRepository = jobListingRepository;
        this.jobSearchRepository = jobSearchRepository;
        this.savedJobRepository = savedJobRepository;
        this.externalJobApiService = externalJobApiService;
    }

    /**
     * Search jobs with filters and caching
     */
    public Page<JobListing> searchJobs(String query, String location, String jobType,
                                     Double minSalary, Double maxSalary, List<String> skills,
                                     int page, int size, String userId) {

        // Check cache first
        SearchCacheEntry cached = searchCache.get(userId);
        if (cached != null && cached.isValid() && cached.matchesSearch(query, location, jobType)) {
            System.out.println("✅ Returning cached results for user: " + userId);
            return getPageFromCache(cached, page, size);
        }

        System.out.println("🔍 Cache miss or expired - fetching fresh data for user: " + userId);
        
        // Save search history
        saveSearchHistory(userId, query, location, jobType, minSalary, maxSalary, skills);
        
        // Fetch jobs from external APIs
        try {
            externalJobApiService.fetchJobsFromAllSources(query, location).get();
        } catch (Exception e) {
            System.err.println("❌ Failed to fetch jobs from external APIs: " + e.getMessage());
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "postedDate"));
        Page<JobListing> results = performSearch(query, location, pageable);

        // Update Cache
        searchCache.put(userId, new SearchCacheEntry(query, location, jobType, results));
        
        return results;
    }


    /**
     * Get cached search results (for when user returns to application)
     */
    public Optional<Page<JobListing>> getCachedSearch(String userId, int page, int size) {
        SearchCacheEntry cached = searchCache.get(userId);
        if (cached != null && cached.isValid()) {
            System.out.println("✅ Returning cached results for user: " + userId);
            return Optional.of(getPageFromCache(cached, page, size));
        }
        return Optional.empty();
    }

    /**
     * Check if user has valid cached results
     */
    public boolean hasCachedResults(String userId) {
        SearchCacheEntry cached = searchCache.get(userId);
        return cached != null && cached.isValid();
    }

    /**
     * Clear cache for user
     */
    public void clearCache(String userId) {
        searchCache.remove(userId);
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
    private Page<JobListing> performSearch(String query, String location, Pageable pageable) {
        if (query != null && !query.trim().isEmpty()) {
            return jobListingRepository.findByTitleContainingIgnoreCase(query.trim(), pageable);
        } else if (location != null && !location.trim().isEmpty()) {
            return jobListingRepository.findByLocationContainingIgnoreCase(location.trim(), pageable);
        } else {
            return jobListingRepository.findAll(pageable);
        }
    }

    private Page<JobListing> getPageFromCache(SearchCacheEntry cached, int page, int size) {
        return cached.getResults();
    }

    /**
     * Save search history for user
     */
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

    // Inner class for cache entry
    private static class SearchCacheEntry {
        private final String query;
        private final String location;
        private final String jobType;
        private final Page<JobListing> results;
        private final Instant cachedAt;

        public SearchCacheEntry(String query, String location, String jobType, Page<JobListing> results) {
            this.query = query;
            this.location = location;
            this.jobType = jobType;
            this.results = results;
            this.cachedAt = Instant.now();
        }

        public boolean isValid() {
            return Instant.now().isBefore(cachedAt.plus(CACHE_VALIDITY_MINUTES, ChronoUnit.MINUTES));
        }

        public boolean matchesSearch(String query, String location, String jobType) {
            return equals(this.query, query) && 
                    equals(this.location, location) && 
                    equals(this.jobType, jobType);
        }

        private boolean equals(String s1, String s2) {
            if (s1 == null) return s2 == null;
            return s1.equals(s2);
        }

        public Page<JobListing> getResults() {
            return results;
        }
    }
}
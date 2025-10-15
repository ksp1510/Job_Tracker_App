// Job Search Service
package com.jobtracker.service;

import com.jobtracker.model.JobListing;
import com.jobtracker.model.JobSearch;
import com.jobtracker.model.SavedJob;
import com.jobtracker.repository.JobListingRepository;
import com.jobtracker.repository.JobSearchRepository;
import com.jobtracker.repository.SavedJobRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
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
        SearchCacheEntry cached = searchCache.remove(userId);
        if (cached != null && cached.isValid() && cached.matchesSearch(query, location, jobType, minSalary, maxSalary, skills)) {
            System.out.println("✅ Returning cached results for user: " + userId + ", page: " + page + ", size: " + size);
            // FIXED: Return correct page from cache
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "postedDate"));
            List<JobListing> allJobs = cached.getResults().getContent();
            int start = Math.min(page * size, allJobs.size());
            int end = Math.min(start + size, allJobs.size());
            List<JobListing> pageContent = allJobs.subList(start, end);

            return new PageImpl<>(pageContent, pageable, allJobs.size());
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

        Pageable pageable = PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "postedDate"));
        Page<JobListing> results = performSearch(query, location, pageable);

        // Update Cache
        searchCache.put(userId, new SearchCacheEntry(query, location, jobType, minSalary, maxSalary, skills, results));
        
        return results;
    }


    /**
     * Get cached search results (for when user returns to application)
     */
    public Optional<Page<JobListing>> getCachedSearch(String userId, int page, int size, String query, String location, String jobType, Double minSalary, Double maxSalary, List<String> skills) {
        SearchCacheEntry cached = searchCache.get(userId);
        if (cached != null && cached.isValid()
                && cached.matchesSearch(query, location, jobType, minSalary, maxSalary, skills)) {
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
    public Optional<SavedJob> saveJob(String userId, String jobListingId, String notes) {
        // Check if already saved
        Optional<SavedJob> existing = savedJobRepository.findByUserIdAndJobListingId(userId, jobListingId);
        if (existing.isPresent()) {
            return existing;
        }
        
        SavedJob savedJob = new SavedJob();
        savedJob.setUserId(userId);
        savedJob.setJobListingId(jobListingId);
        savedJob.setNotes(notes);
        savedJob.setSavedAt(LocalDateTime.now());
        
        return Optional.of(savedJobRepository.save(savedJob));
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
        if (location != null && !location.isEmpty()) {
            Point geoPoint = geocodeLocation(location);
            if (geoPoint != null) {
                Distance distance = new Distance(25, Metrics.KILOMETERS);
                List<JobListing> nearby = jobListingRepository.findByLocationNear(geoPoint, distance);
                return new PageImpl<>(nearby, pageable, nearby.size());
            }
        }
    
        if (query != null && !query.trim().isEmpty()) {
            return jobListingRepository.findByTitleContainingIgnoreCase(query.trim(), pageable);
        }
        return jobListingRepository.findAll(pageable);
    }

    private Point geocodeLocation(String location) {
        try {
            String encoded =  URLEncoder.encode(location, StandardCharsets.UTF_8);
            String url = "https://nominatim.openstreetmap.org/search?q=" + encoded + "&format=json&limit=1";
            RestTemplate rest = new RestTemplate();
            ResponseEntity<List<Map<String, Object>>> response = rest.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );

            List<Map<String, Object>> results = response.getBody();
            if (results != null && !results.isEmpty()) {
                var obj = (Map<String, Object>) results.get(0);
                double lat = Double.parseDouble((String) obj.get("lat"));
                double lon = Double.parseDouble((String) obj.get("lon"));
                return new Point(lon, lat);
            }
        } catch (Exception e) {
            System.err.println("Geocoding failed for " + location + ": " + e.getMessage());
        }
        return null;
    }



    private Page<JobListing> getPageFromCache(SearchCacheEntry cached, int page, int size) {
        Page<JobListing> allResults = cached.getResults();
        List<JobListing> allJobs = allResults.getContent();
        
        // Calculate pagination
        int start = page * size;
        int end = Math.min(start + size, allJobs.size());
        
        // Handle out of bounds
        if (start >= allJobs.size()) {
            return new PageImpl<>(List.of(), PageRequest.of(page, size), allJobs.size());
        }
        
        // Extract the requested page
        List<JobListing> pageContent = allJobs.subList(start, end);
        
        System.out.println("📄 Extracting page " + page + " (items " + start + "-" + end + " of " + allJobs.size() + ")");
        
        return new PageImpl<>(pageContent, PageRequest.of(page, size), allJobs.size());
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
        private final Double minSalary;
        private final Double maxSalary;
        private final List<String> skills;
        private final Page<JobListing> results;
        private final Instant cachedAt;

        public SearchCacheEntry(String query, String location, String jobType, Double minSalary, Double maxSalary, List<String> skills, Page<JobListing> results) {
            this.query = query;
            this.location = location;
            this.jobType = jobType;
            this.minSalary = minSalary;
            this.maxSalary = maxSalary;
            this.skills = skills;
            this.results = results;
            this.cachedAt = Instant.now();
        }

        public boolean isValid() {
            return Instant.now().isBefore(cachedAt.plus(CACHE_VALIDITY_MINUTES, ChronoUnit.MINUTES));
        }

        public boolean matchesSearch(String query, String location, String jobType,
                             Double minSalary, Double maxSalary, List<String> skills) {
            return equals(this.query, query)
                && equals(this.location, location)
                && equals(this.jobType, jobType)
                && equals(this.minSalary, minSalary)
                && equals(this.maxSalary, maxSalary)
                && equalsList(this.skills, skills);
        }

        private boolean equals(String s1, String s2) {
            if (s1 == null) return s2 == null;
            return s1.equals(s2);
        }

        private boolean equals(Double d1, Double d2) {
            if (d1 == null) return d2 == null;
            return d1.equals(d2);
        }

        private boolean equalsList(List<String> a, List<String> b) {
            if (a == null && b == null) return true;
            if (a == null || b == null) return false;
            return a.containsAll(b) && b.containsAll(a);
        }


        public Page<JobListing> getResults() {
            return results;
        }
    }
}
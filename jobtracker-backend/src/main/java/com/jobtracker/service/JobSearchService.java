// Job Search Service
package com.jobtracker.service;

import java.util.regex.Pattern;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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

        // Generate cache key
        String cacheKey = userId + "_" + query + "_" + location + "_" + jobType + "_" + minSalary + "_" + maxSalary + "_" + skills;

        // Check cache first
        SearchCacheEntry cached = searchCache.get(userId);
        if (cached != null && cached.isValid() && cached.matchesSearch(query, location)) {
            System.out.println("✅ Using valid cached results for user: " + userId);
            return getPageFromCache(cached, page, size);
        }

        synchronized (this) {
            // Double-check after acquiring lock
            cached = searchCache.get(userId);
            if (cached != null && cached.isValid() && cached.matchesSearch(query, location)) {
                System.out.println("✅ Using cached results (after lock) for user: " + userId);
                return getPageFromCache(cached, page, size);
            }

            System.out.println("🔍 Cache miss or expired - fetching fresh data for user: " + userId);

            boolean hasQuery = query != null && !query.trim().isEmpty();
            boolean hasLocation = location != null && !location.trim().isEmpty();
            
            if (hasQuery || hasLocation) {
                System.out.println("🔍 Starting job fetch from all sources: " + query + " in " + location);
                
                // Save search history
                saveSearchHistory(userId, query, location);
                
                // Fetch jobs from external APIs
                try {
                    externalJobApiService.fetchJobsFromAllSources(
                        query, location, jobType, minSalary, maxSalary, skills).get();
                } catch (Exception e) {
                    System.err.println("❌ Failed to fetch jobs from external APIs: " + e.getMessage());
                }
            } else {
                System.out.println("ℹ️ No search terms provided - returning existing jobs from DB");
            }

            Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Direction.DESC, "postedDate"));
            Page<JobListing> results = performSearch(query, location, pageable);
            
            System.out.println("📊 Search returned " + results.getTotalElements() + " jobs");
            
            searchCache.put(userId, new SearchCacheEntry(query, location, results));
            return getPageFromCache(searchCache.get(userId), page, size);
        }
    }


    /**
     * Get cached search results (for when user returns to application)
     */
    private Page<JobListing> getPageFromCache(SearchCacheEntry cached, int page, int size) {
        List<JobListing> allJobs = cached.getJobs();
        
        int totalElements = allJobs.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int start = page * size;
        int end = Math.min(start + size, totalElements);
        
        System.out.println("📄 Cache pagination - Page: " + page + ", Size: " + size);
        System.out.println("📄 Total jobs: " + totalElements + ", Total pages: " + totalPages);
        System.out.println("📄 Extracting items " + start + " to " + end);
        
        List<JobListing> pageContent;
        if (start >= totalElements) {
            System.out.println("⚠️ Page out of bounds - returning empty");
            pageContent = Collections.emptyList();
        } else {
            pageContent = allJobs.subList(start, end);
            System.out.println("✅ Returning " + pageContent.size() + " jobs for page " + page);
        }
        
        return new PageImpl<>(
            pageContent,
            PageRequest.of(page, size),
            totalElements
        );
    }

    /**
     * Check if user has valid cached results
     */
    public boolean hasCachedResults(String userId) {
        SearchCacheEntry cached = searchCache.get(userId);
        return cached != null && cached.isValid();
    }

    /**
     * Get cached search results for pagination
     */
    public Optional<Page<JobListing>> getCachedSearch(
            String userId, 
            int page, 
            int size, 
            String query, 
            String location) {
        
        SearchCacheEntry cached = searchCache.get(userId);
        
        if (cached != null && cached.isValid()) {
            // ✅ If no params provided, return whatever is cached (for pagination)
            boolean noParamsProvided = (query == null || query.trim().isEmpty()) && 
                                    (location == null || location.trim().isEmpty());
            
            if (noParamsProvided) {
                System.out.println("✅ No params provided - returning cached page " + page + " for user: " + userId);
                return Optional.of(getPageFromCache(cached, page, size));
            }
            
            // If params provided, they must match
            if (cached.matchesSearch(query, location)) {
                System.out.println("✅ Params match - returning cached page " + page + " for user: " + userId);
                return Optional.of(getPageFromCache(cached, page, size));
            }
            
            System.out.println("❌ Cache params don't match - requested: '" + query + "' in '" + location + "'");
        }
        
        System.out.println("❌ No valid cache found for user: " + userId);
        return Optional.empty();
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
        return jobSearchRepository.findByUserIdOrderBySearchedAtDesc(userId);
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

        searchCache.clear();
        System.out.println("🗑️ Cleared search cache due to job cleanup");
    }

    // Private helper methods
    private Page<JobListing> performSearch(String query, String location, Pageable pageable) {
        boolean hasQuery = query != null && !query.isBlank();
        boolean hasLocation = location != null && !location.isBlank();

        if (!pageable.getSort().isSorted()) {
            pageable = PageRequest.of(
                pageable.getPageNumber(), 
                pageable.getPageSize(), 
                Sort.by(Sort.Direction.DESC, "postedDate"));
        }
        
        if (hasQuery && hasLocation) {
            // ✅ Split into keywords for flexible matching
            String[] keywords = query.trim().split("\\s+");
            String queryPattern = "(?i).*" + String.join(".*", 
                Arrays.stream(keywords)
                    .map(Pattern::quote)
                    .toArray(String[]::new)
            ) + ".*";
            
            String locationPattern = "(?i).*" + Pattern.quote(location.trim()) + ".*";
            
            // 🔍 DEBUG LOGGING
            System.out.println("=== SEARCH DEBUG ===");
            System.out.println("Query: " + query);
            System.out.println("Pattern: " + queryPattern);
            System.out.println("Location: " + location);
            System.out.println("Location Pattern: " + locationPattern);
            System.out.println("Sort: Most recent first (DESC postedDate)");
            
            // Check total active jobs
            long totalActive = jobListingRepository.countByIsActiveTrue();
            System.out.println("📊 Total active jobs in DB: " + totalActive);
            
            Page<JobListing> results = jobListingRepository.findByIsActiveTrueAndTitleRegexAndLocationRegex(
                queryPattern,
                locationPattern,
                pageable);
            
            System.out.println("📊 Jobs matching filter: " + results.getTotalElements());
            
            // Sample results
            if (results.hasContent()) {
                System.out.println("📋 Sample matched jobs:");
                results.getContent().stream().limit(3).forEach(job -> {
                    String postedAgo = job.getPostedDate() != null
                        ? formatTimeAgo(job.getPostedDate())
                        : "Unknown";
                    System.out.println("  - " + job.getTitle() + " @ " + job.getLocation() + " (Posted " + postedAgo + ")");
                });
            }
            
            System.out.println("===================\n");
            
            return results;
        }

        if (hasQuery) {
            String[] keywords = query.trim().split("\\s+");
            String queryPattern = "(?i).*" + String.join(".*", 
                Arrays.stream(keywords)
                    .map(Pattern::quote)
                    .toArray(String[]::new)
            ) + ".*";
            
            return jobListingRepository.findByIsActiveTrueAndTitleRegex(
                queryPattern, pageable);
        }
        
        if (hasLocation) {
            Point geoPoint = geocodeLocation(location);
            if (geoPoint != null) {
                Distance distance = new Distance(25, Metrics.KILOMETERS);
                List<JobListing> nearby = jobListingRepository.findByIsActiveTrueAndLocationNear(
                    geoPoint, distance);

                nearby.sort((a, b) -> {
                    if (a.getPostedDate() == null) return 1;
                    if (b.getPostedDate() == null) return -1;
                    return b.getPostedDate().compareTo(a.getPostedDate());
                });
                return new PageImpl<>(nearby, pageable, nearby.size());
            }
            
            return jobListingRepository.findByIsActiveTrueAndLocationContainingIgnoreCase(
                location.trim(), pageable);
        }
        
        return jobListingRepository.findByIsActiveTrue(pageable);
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

    // ✅ Add helper method for time formatting
    private String formatTimeAgo(Instant instant) {
        if (instant == null) return "unknown";
        
        long hours = ChronoUnit.HOURS.between(instant, Instant.now());
        if (hours < 1) return "just now";
        if (hours < 24) return hours + "h ago";
        
        long days = hours / 24;
        if (days < 7) return days + "d ago";
        
        long weeks = days / 7;
        if (weeks < 4) return weeks + "w ago";
        
        long months = days / 30;
        return months + "mo ago";
    }

    /**
     * Save search history for user
     */
    private void saveSearchHistory(String userId, String query, String location) {
        JobSearch search = new JobSearch();
        search.setUserId(userId);
        search.setQuery(query);
        search.setLocation(location);
        search.setSearchedAt(Instant.now());
        
        jobSearchRepository.save(search);
    }

    // Inner class for cache entry
    private static class SearchCacheEntry {
        private final String query;
        private final String location;
        private final List<JobListing> jobs;  // ✅ Changed from Page to List
        private final Instant cachedAt;

        public SearchCacheEntry(String query, String location, Page<JobListing> results) {
            this.query = query;
            this.location = location;
            this.jobs = new ArrayList<>(results.getContent());  // ✅ Convert Page content to List
            this.cachedAt = Instant.now();
        }

        public boolean isValid() {
            return Instant.now().isBefore(cachedAt.plus(CACHE_VALIDITY_MINUTES, ChronoUnit.MINUTES));
        }

        public boolean matchesSearch(String query, String location) {
            return equals(this.query, query)
                && equals(this.location, location);
        }

        private boolean equals(String s1, String s2) {
            if (s1 == null) return s2 == null;
            return s1.equals(s2);
        }

        public List<JobListing> getJobs() {  // ✅ Changed return type from Page to List
            return jobs;
        }
    }
}
package com.jobtracker.controller;

import com.jobtracker.config.JwtUtil;
import com.jobtracker.model.JobListing;
import com.jobtracker.model.JobSearch;
import com.jobtracker.model.SavedJob;
import com.jobtracker.service.JobSearchService;
import com.jobtracker.exception.ResourceNotFoundException;
import lombok.Data;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/jobs")
public class JobSearchController {

    private final JobSearchService jobSearchService;
    private final JwtUtil jwtUtil;

    public JobSearchController(JobSearchService jobSearchService, JwtUtil jwtUtil) {
        this.jobSearchService = jobSearchService;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Check if user has cached search results
     */
    @GetMapping("/cache/status")
    public ResponseEntity<CacheStatusResponse> checkCacheStatus(
            @RequestHeader("Authorization") String authHeader) {
        String userId = extractUserId(authHeader);
        boolean hasCached = jobSearchService.hasCachedResults(userId);
        return ResponseEntity.ok(new CacheStatusResponse(hasCached));
    }

    /**
     * Get cached search results
     */
    @GetMapping("/cache")
    public ResponseEntity<PaginatedJobResponse> getCachedSearch(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader("Authorization") String authHeader) {
        String userId = extractUserId(authHeader);
        Optional<Page<JobListing>> cachedResults = jobSearchService.getCachedSearch(userId, page, size);
        
        if (cachedResults.isPresent()) {
            Page<JobListing> jobs = cachedResults.get();
            PaginatedJobResponse response = new PaginatedJobResponse(
                jobs.getContent(),
                jobs.getTotalElements(),
                jobs.getTotalPages(),
                jobs.getNumber(),
                jobs.getSize()
            );
            return ResponseEntity.ok(response);
        }
        
        return ResponseEntity.noContent().build();
    }

    /**
     * Search jobs with filters - FIXED: Returns custom DTO instead of Spring Page
     */
    @GetMapping("/search")
    public ResponseEntity<PaginatedJobResponse> searchJobs(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String jobType,
            @RequestParam(required = false) Double minSalary,
            @RequestParam(required = false) Double maxSalary,
            @RequestParam(required = false) List<String> skills,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader("Authorization") String authHeader) {
        
        String userId = extractUserId(authHeader);
        Page<JobListing> jobs = jobSearchService.searchJobs(
                query, location, jobType, minSalary, maxSalary, skills, page, size, userId);
        
        // Convert Spring Page to custom DTO
        PaginatedJobResponse response = new PaginatedJobResponse(
            jobs.getContent(),
            jobs.getTotalElements(),
            jobs.getTotalPages(),
            jobs.getNumber(),  // Spring uses 'number' for current page
            jobs.getSize()
        );
        
        return ResponseEntity.ok(response);
    }

    /**
     * Clear cached search results
     */
    @DeleteMapping("/cache")
    public ResponseEntity<Void> clearCache(
            @RequestHeader("Authorization") String authHeader) {
        String userId = extractUserId(authHeader);
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
     * Save/bookmark a job - FIXED: Removed userId from body, returns SavedJob instead of Optional
     */
    @PostMapping("/{id}/save")
    public ResponseEntity<SavedJob> saveJob(
            @PathVariable String id,
            @RequestBody(required = false) SaveJobRequest request,
            @RequestHeader("Authorization") String authHeader) {
        
        String userId = extractUserId(authHeader);
        String notes = request != null ? request.getNotes() : null;
        
        SavedJob savedJob = jobSearchService.saveJob(userId, id, notes)
            .orElseThrow(() -> new RuntimeException("Failed to save job"));
        
        return ResponseEntity.ok(savedJob);
    }

    /**
     * Get user's saved jobs
     */
    @GetMapping("/saved")
    public ResponseEntity<List<SavedJob>> getSavedJobs(
            @RequestHeader("Authorization") String authHeader) {
        
        String userId = extractUserId(authHeader);
        return ResponseEntity.ok(jobSearchService.getSavedJobs(userId));
    }

    /**
     * Remove saved job
     */
    @DeleteMapping("/saved/{id}")
    public ResponseEntity<Void> unsaveJob(
            @PathVariable String id,
            @RequestHeader("Authorization") String authHeader) {
        
        String userId = extractUserId(authHeader);
        jobSearchService.unsaveJob(userId, id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get search history
     */
    @GetMapping("/search-history")
    public ResponseEntity<List<JobSearch>> getSearchHistory(
            @RequestHeader("Authorization") String authHeader) {
        
        String userId = extractUserId(authHeader);
        return ResponseEntity.ok(jobSearchService.getSearchHistory(userId));
    }

    // Helper method
    private String extractUserId(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return jwtUtil.getUserId(token);
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

    // FIXED: Custom pagination response DTO to match frontend expectations
    @Data
    static class PaginatedJobResponse {
        private final List<JobListing> content;
        private final long totalElements;
        private final int totalPages;
        private final int page;  // Current page number
        private final int size;
    }
}
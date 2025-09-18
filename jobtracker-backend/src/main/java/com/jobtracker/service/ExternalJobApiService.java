package com.jobtracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobtracker.model.JobListing;
import com.jobtracker.repository.JobListingRepository;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class ExternalJobApiService {

    private final WebClient webClient;
    private final JobListingRepository jobListingRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.api.serpapi.key:}")
    private String serpApiKey;

    @Value("${app.api.rapidapi.key:}")
    private String rapidApiKey;

    @Value("${app.api.theirstack.key:}")
    private String theirStackKey;

    // Validation method
    @PostConstruct
    public void validateApiKeys() {
        if (serpApiKey.isEmpty()) {
            System.out.println("⚠️ SERPAPI_KEY not configured");
        }
        if (rapidApiKey.isEmpty()) {
            System.out.println("⚠️ RAPIDAPI_KEY not configured");
        }
        if (theirStackKey.isEmpty()) {
            System.out.println("⚠️ THEIRSTACK_KEY not configured");
        }
    }

    public ExternalJobApiService(WebClient.Builder webClientBuilder, 
                               JobListingRepository jobListingRepository) {
        this.webClient = webClientBuilder
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024 * 10)) // 10MB
                .build();
        this.jobListingRepository = jobListingRepository;
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Fetch jobs from all external APIs
     */
    public CompletableFuture<Void> fetchJobsFromAllSources(String query, String location) {
        System.out.println("🔍 Starting job fetch from all sources: " + query + " in " + location);
        
        return CompletableFuture.allOf(
                fetchFromJSearchAPI(query, location),
                fetchFromSerpAPI(query, location),
                fetchFromTheirStackAPI(query, location)
        );
    }

    /**
     * Fetch from JSearch (RapidAPI)
     * https://rapidapi.com/letscrape-6bRBa3QguO5/api/jsearch
     */
    private CompletableFuture<Void> fetchFromJSearchAPI(String query, String location) {
        return CompletableFuture.runAsync(() -> {
            try {
                if (rapidApiKey.isEmpty()) {
                    System.out.println("⚠️ RapidAPI key not configured for JSearch");
                    return;
                }

                System.out.println("🔍 Fetching from JSearch API...");

                Mono<String> response = webClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .scheme("https")
                                .host("jsearch.p.rapidapi.com")
                                .path("/search")
                                .queryParam("query", query + " " + location)
                                .queryParam("page", "1")
                                .queryParam("num_pages", "3")
                                .build())
                        .header("X-RapidAPI-Key", rapidApiKey)
                        .header("X-RapidAPI-Host", "jsearch.p.rapidapi.com")
                        .retrieve()
                        .bodyToMono(String.class);

                String responseBody = response.block();
                if (responseBody != null) {
                    List<JobListing> jobs = parseJSearchResponse(responseBody);
                    jobListingRepository.saveAll(jobs);
                    System.out.println("✅ Saved " + jobs.size() + " jobs from JSearch");
                }

            } catch (Exception e) {
                System.err.println("❌ JSearch API error: " + e.getMessage());
            }
        });
    }

    /**
     * Fetch from SerpAPI Google Jobs
     * https://serpapi.com/google-jobs-api
     */
    private CompletableFuture<Void> fetchFromSerpAPI(String query, String location) {
        return CompletableFuture.runAsync(() -> {
            try {
                if (serpApiKey.isEmpty()) {
                    System.out.println("⚠️ SerpAPI key not configured");
                    return;
                }

                System.out.println("🔍 Fetching from SerpAPI...");

                Mono<String> response = webClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .scheme("https")
                                .host("serpapi.com")
                                .path("/search.json")
                                .queryParam("engine", "google_jobs")
                                .queryParam("q", query)
                                .queryParam("location", location)
                                .queryParam("api_key", serpApiKey)
                                .queryParam("num", "20")
                                .build())
                        .retrieve()
                        .bodyToMono(String.class);

                String responseBody = response.block();
                if (responseBody != null) {
                    List<JobListing> jobs = parseSerpAPIResponse(responseBody);
                    jobListingRepository.saveAll(jobs);
                    System.out.println("✅ Saved " + jobs.size() + " jobs from SerpAPI");
                }

            } catch (Exception e) {
                System.err.println("❌ SerpAPI error: " + e.getMessage());
            }
        });
    }

    /**
     * Fetch from TheirStack
     * Note: Replace with actual TheirStack API endpoint
     */
    private CompletableFuture<Void> fetchFromTheirStackAPI(String query, String location) {
        return CompletableFuture.runAsync(() -> {
            try {
                if (theirStackKey.isEmpty()) {
                    System.out.println("⚠️ TheirStack API key not configured");
                    return;
                }

                System.out.println("🔍 Fetching from TheirStack API...");

                // TODO: Replace with actual TheirStack API endpoint
                Mono<String> response = webClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .scheme("https")
                                .host("api.theirstack.com") // Replace with actual endpoint
                                .path("/jobs")
                                .queryParam("q", query)
                                .queryParam("location", location)
                                .queryParam("api_key", theirStackKey)
                                .build())
                        .retrieve()
                        .bodyToMono(String.class);

                String responseBody = response.block();
                if (responseBody != null) {
                    List<JobListing> jobs = parseTheirStackResponse(responseBody);
                    jobListingRepository.saveAll(jobs);
                    System.out.println("✅ Saved " + jobs.size() + " jobs from TheirStack");
                }

            } catch (Exception e) {
                System.err.println("❌ TheirStack API error: " + e.getMessage());
            }
        });
    }

    // JSON Parsers for each API
    private List<JobListing> parseJSearchResponse(String json) {
        List<JobListing> jobs = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode jobsArray = root.get("data");

            if (jobsArray != null && jobsArray.isArray()) {
                for (JsonNode jobNode : jobsArray) {
                    JobListing job = new JobListing();
                    job.setExternalId("JSEARCH-" + jobNode.get("job_id").asText());
                    job.setTitle(jobNode.get("job_title").asText());
                    job.setCompany(jobNode.get("employer_name").asText());
                    job.setLocation(jobNode.get("job_city").asText() + ", " + 
                                  jobNode.get("job_state").asText());
                    job.setDescription(jobNode.get("job_description").asText());
                    job.setJobType(jobNode.get("job_employment_type").asText());
                    job.setApplyUrl(jobNode.get("job_apply_link").asText());
                    job.setSource("JSEARCH");
                    
                    // Parse salary if available
                    if (jobNode.has("job_min_salary")) {
                        job.setSalary(jobNode.get("job_min_salary").asDouble());
                    }
                    
                    // Parse posted date
                    String postedDate = jobNode.get("job_posted_at_datetime_utc").asText();
                    job.setPostedDate(Instant.parse(postedDate));
                    job.setActive(true);
                    
                    jobs.add(job);
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Error parsing JSearch response: " + e.getMessage());
        }
        return jobs;
    }

    private List<JobListing> parseSerpAPIResponse(String json) {
        List<JobListing> jobs = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode jobsArray = root.get("jobs_results");

            if (jobsArray != null && jobsArray.isArray()) {
                for (JsonNode jobNode : jobsArray) {
                    JobListing job = new JobListing();
                    job.setExternalId("SERP-" + jobNode.get("job_id").asText());
                    job.setTitle(jobNode.get("title").asText());
                    job.setCompany(jobNode.get("company_name").asText());
                    job.setLocation(jobNode.get("location").asText());
                    job.setDescription(jobNode.get("description").asText());
                    job.setSource("SERPAPI");
                    
                    // Parse salary range if available
                    if (jobNode.has("salary")) {
                        job.setSalaryRange(jobNode.get("salary").asText());
                    }
                    
                    // Set default posted date (SerpAPI doesn't always provide this)
                    job.setPostedDate(Instant.now().minusSeconds(86400)); // 1 day ago
                    job.setActive(true);
                    
                    jobs.add(job);
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Error parsing SerpAPI response: " + e.getMessage());
        }
        return jobs;
    }

    private List<JobListing> parseTheirStackResponse(String json) {
        List<JobListing> jobs = new ArrayList<>();
        try {
            // TODO: Implement based on TheirStack API response format
            JsonNode root = objectMapper.readTree(json);
            // Parse TheirStack specific JSON structure
            System.out.println("🔄 Parsing TheirStack response...");
        } catch (Exception e) {
            System.err.println("❌ Error parsing TheirStack response: " + e.getMessage());
        }
        return jobs;
    }

    /**
     * Manual job fetch trigger (for testing)
     */
    public CompletableFuture<String> fetchJobsManually(String query, String location) {
        return fetchJobsFromAllSources(query, location)
                .thenApply(v -> "Job fetch completed for: " + query + " in " + location);
    }
}
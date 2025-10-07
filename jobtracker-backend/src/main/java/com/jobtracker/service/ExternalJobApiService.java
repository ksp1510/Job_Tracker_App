package com.jobtracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobtracker.model.JobListing;
import com.jobtracker.repository.JobListingRepository;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
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

    public ExternalJobApiService(WebClient.Builder webClientBuilder, 
                               JobListingRepository jobListingRepository) {
        this.webClient = webClientBuilder
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024 * 10))
                .build();
        this.jobListingRepository = jobListingRepository;
        this.objectMapper = new ObjectMapper();
    }

    @PostConstruct
    public void debugConfiguration() {
        System.out.println("🔧 ExternalJobApiService Configuration:");
        System.out.println("   SerpAPI key length: " + (serpApiKey != null ? serpApiKey.length() : "null"));
        System.out.println("   RapidAPI key length: " + (rapidApiKey != null ? rapidApiKey.length() : "null"));
        
        System.out.println("🔧 Direct Environment Variables:");
        System.out.println("   SERPAPI_KEY: " + (System.getenv("SERPAPI_KEY") != null ? "SET" : "NOT_SET"));
        System.out.println("   RAPIDAPI_KEY: " + (System.getenv("RAPIDAPI_KEY") != null ? "SET" : "NOT_SET"));  
    }

    /**
     * FIXED: Helper method to determine country code from location
     */
    private String getCountryCode(String location) {
        if (location == null) return "us";
        
        String lowerLocation = location.toLowerCase();
        
        // India
        if (lowerLocation.contains("india") || lowerLocation.contains("ahmedabad") || 
            lowerLocation.contains("mumbai") || lowerLocation.contains("delhi") ||
            lowerLocation.contains("bangalore") || lowerLocation.contains("hyderabad") ||
            lowerLocation.contains("pune") || lowerLocation.contains("chennai") ||
            lowerLocation.contains("kolkata")) {
            return "in";
        }
        
        // Canada
        if (lowerLocation.contains("canada") || lowerLocation.contains("toronto") ||
            lowerLocation.contains("vancouver") || lowerLocation.contains("montreal")) {
            return "ca";
        }
        
        // UK
        if (lowerLocation.contains("uk") || lowerLocation.contains("united kingdom") ||
            lowerLocation.contains("london") || lowerLocation.contains("manchester")) {
            return "gb";
        }
        
        // Australia
        if (lowerLocation.contains("australia") || lowerLocation.contains("sydney") ||
            lowerLocation.contains("melbourne")) {
            return "au";
        }
        
        // Default to US
        return "us";
    }

    /**
     * Test API connections
     */
    public CompletableFuture<String> testApiConnections() {
        return CompletableFuture.supplyAsync(() -> {
            StringBuilder results = new StringBuilder();
            
            results.append("=== SerpAPI ===\n");
            if (serpApiKey.isEmpty()) {
                results.append("❌ SerpAPI key not configured\n");
            } else {
                results.append("✅ SerpAPI key configured (length: ").append(serpApiKey.length()).append(")\n");
                try {
                    testSerpAPIConnection();
                    results.append("✅ SerpAPI connection successful\n");
                } catch (Exception e) {
                    results.append("❌ SerpAPI connection failed: ").append(e.getMessage()).append("\n");
                }
            }
            
            results.append("=== RapidAPI ===\n");
            if (rapidApiKey.isEmpty()) {
                results.append("❌ RapidAPI key not configured\n");
            } else {
                results.append("✅ RapidAPI key configured (length: ").append(rapidApiKey.length()).append(")\n");
                try {
                    testRapidAPIConnection();
                    results.append("✅ RapidAPI connection successful\n");
                } catch (Exception e) {
                    results.append("❌ RapidAPI connection failed: ").append(e.getMessage()).append("\n");
                }
            }

            return results.toString();
        });
    }

    private void testSerpAPIConnection() throws Exception {
        try {
            String response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("serpapi.com")
                            .path("/search.json")
                            .queryParam("engine", "google_jobs")
                            .queryParam("q", "test")
                            .queryParam("location", "united states")
                            .queryParam("api_key", serpApiKey)
                            .queryParam("num_pages", "1")
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            System.out.println("✅ SerpAPI Response received: " + (response != null ? "Success" : "No response"));
        } catch (WebClientResponseException e) {
            System.err.println("❌ SerpAPI Error - Status: " + e.getStatusCode());
            System.err.println("❌ SerpAPI Error - Response: " + e.getResponseBodyAsString());
            throw new RuntimeException("SerpAPI test failed: " + e.getStatusCode());
        }
    }

    private void testRapidAPIConnection() throws Exception {
        try {
            String response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("jsearch.p.rapidapi.com")
                            .path("/search")
                            .queryParam("query", "test")
                            .queryParam("page", "1")
                            .queryParam("num_pages", "1")
                            .build())
                    .header("X-RapidAPI-Key", rapidApiKey)
                    .header("X-RapidAPI-Host", "jsearch.p.rapidapi.com")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            System.out.println("✅ RapidAPI Response received: " + (response != null ? "Success" : "No response"));
        } catch (WebClientResponseException e) {
            System.err.println("❌ RapidAPI Error - Status: " + e.getStatusCode());
            System.err.println("❌ RapidAPI Error - Response: " + e.getResponseBodyAsString());
            throw new RuntimeException("RapidAPI test failed: " + e.getStatusCode());
        }
    }
     
    /**
     * Fetch jobs from all external APIs
     */
    public CompletableFuture<Void> fetchJobsFromAllSources(String query, String location) {
        System.out.println("🔍 Starting job fetch from all sources: " + query + " in " + location);
        
        return CompletableFuture.allOf(
                fetchFromJSearchAPI(query, location),
                fetchFromSerpAPI(query, location)
        );
    }

    /**
 * Fetch from JSearch (RapidAPI) - FIXED: Better location handling
 */
private CompletableFuture<Void> fetchFromJSearchAPI(String query, String location) {
    return CompletableFuture.runAsync(() -> {            
        try {
            if (rapidApiKey.isEmpty()) {
                System.out.println("⚠️ RapidAPI key not configured for JSearch");
                return;
            }

            System.out.println("🔍 Fetching from JSearch API: " + query + " in " + location);

            // Build search query with location - make it final
            final String searchQuery = (location != null && !location.isEmpty()) 
                ? query + " in " + location 
                : query;

            Mono<String> response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("jsearch.p.rapidapi.com")
                            .path("/search")
                            .queryParam("query", searchQuery)
                            .queryParam("page", "1")
                            .queryParam("num_pages", "2")
                            .build())
                    .header("X-RapidAPI-Key", rapidApiKey)
                    .header("X-RapidAPI-Host", "jsearch.p.rapidapi.com")
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    System.err.println("❌ JSearch API 4xx error: " + errorBody);
                                    return Mono.error(new RuntimeException("JSearch API 4xx error: " + errorBody));
                                }))
                    .onStatus(status -> status.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    System.err.println("❌ JSearch API 5xx error: " + errorBody);
                                    return Mono.error(new RuntimeException("JSearch API 5xx error: " + errorBody));
                                }))
                    .bodyToMono(String.class);

            String responseBody = response.block();
            if (responseBody != null) {
                List<JobListing> jobs = parseJSearchResponse(responseBody);
                if (!jobs.isEmpty()) {
                    jobListingRepository.saveAll(jobs);
                    System.out.println("✅ Saved " + jobs.size() + " jobs from JSearch");
                } else {
                    System.out.println("❌ No jobs found from JSearch");
                }
            }

        } catch (Exception e) {
            System.err.println("❌ JSearch API error: " + e.getMessage());
        }
    });
}

    /**
     * Fetch from SerpAPI Google Jobs - FIXED: Added country code support
     */
    private CompletableFuture<Void> fetchFromSerpAPI(String query, String location) {
        return CompletableFuture.runAsync(() -> {
            try {
                if (serpApiKey.isEmpty()) {
                    System.out.println("⚠️ SerpAPI key not configured");
                    return;
                }

                System.out.println("🔍 Fetching from SerpAPI: " + query + " in " + location);

                String countryCode = getCountryCode(location);
                System.out.println("🌍 Using country code: " + countryCode + " for location: " + location);

                Mono<String> response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("serpapi.com")
                        .path("/search.json")
                        .queryParam("engine", "google_jobs")
                        .queryParam("q", query.trim())
                        .queryParam("location", location)
                        .queryParam("gl", countryCode)  // Country code
                        .queryParam("hl", "en")         // Language
                        .queryParam("api_key", serpApiKey)
                        .queryParam("num", "10")
                        .build())
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    System.err.println("❌ SerpAPI error: " + errorBody);
                                    return Mono.error(new RuntimeException("SerpAPI error: " + errorBody));
                                }))
                    .onStatus(status -> status.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    System.err.println("❌ SerpAPI error: " + errorBody);
                                    return Mono.error(new RuntimeException("SerpAPI error: " + errorBody));
                                }))
                    .bodyToMono(String.class);

                String responseBody = response.block();
                if (responseBody != null) {
                    List<JobListing> jobs = parseSerpAPIResponse(responseBody);
                    if (!jobs.isEmpty()) {
                        jobListingRepository.saveAll(jobs);
                        System.out.println("✅ Saved " + jobs.size() + " jobs from SerpAPI");
                    } else {
                        System.out.println("❌ No jobs found from SerpAPI");
                    }
                }

            } catch (Exception e) {
                System.err.println("❌ SerpAPI error: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

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
                    
                    // FIXED: Convert to Double instead of String
                    if (jobNode.has("job_min_salary") && !jobNode.get("job_min_salary").isNull()) {
                        job.setSalary(jobNode.get("job_min_salary").asDouble());
                    }
                    
                    if (jobNode.has("job_posted_at_datetime_utc") && !jobNode.get("job_posted_at_datetime_utc").isNull()) {
                        try {
                            job.setPostedDate(Instant.parse(jobNode.get("job_posted_at_datetime_utc").asText()));   
                        } catch (Exception e) {
                            job.setPostedDate(Instant.now().minusSeconds(86400));
                        }
                    } else {
                        job.setPostedDate(Instant.now().minusSeconds(86400));
                    }
    
                    job.setActive(true);
                    jobs.add(job);
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Error parsing JSearch response: " + e.getMessage());
            e.printStackTrace();
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
                    job.setExternalId("SERP-" + jobNode.path("job_id").asText());
                    job.setTitle(jobNode.path("title").asText());
                    job.setCompany(jobNode.path("company_name").asText());
                    job.setLocation(jobNode.path("location").asText());
                    job.setDescription(jobNode.path("description").asText());
                    job.setSource("SERPAPI");
                    
                    if (jobNode.has("salary") && !jobNode.get("salary").isNull()) {
                        job.setSalaryRange(jobNode.get("salary").asText());
                    }
                    
                    job.setPostedDate(Instant.now().minusSeconds(86400));
                    job.setActive(true);
                    jobs.add(job);
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Error parsing SerpAPI response: " + e.getMessage());
            e.printStackTrace();
        }
        return jobs;
    }

    /**
     * Manual job fetch trigger
     */
    public CompletableFuture<String> fetchJobsManually(String query, String location) {
        return fetchJobsFromAllSources(query, location)
                .thenApply(v -> {
                    String result = "Job fetch completed for: " + query + " in " + location;
                    System.out.println("✅ " + result);
                    return result;
                })
                .exceptionally(throwable -> {
                    String error = "Job fetch failed: " + throwable.getMessage();
                    System.err.println("❌ " + error);
                    return error;
                });
    }
}
package com.jobtracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobtracker.model.JobListing;
import com.jobtracker.repository.JobListingRepository;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.Map;

@Service
public class ExternalJobApiService {

    private final WebClient webClient;
    private final JobListingRepository jobListingRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.api.serpapi.key:}")
    private String serpApiKey;

    @Value("${app.api.rapidapi.key:}")
    private String rapidApiKey;


    // Add debug method to check values
    @PostConstruct
    public void debugConfiguration() {
        System.out.println("🔧 ExternalJobApiService Configuration:");
        System.out.println("   SerpAPI key length: " + (serpApiKey != null ? serpApiKey.length() : "null"));
        System.out.println("   RapidAPI key length: " + (rapidApiKey != null ? rapidApiKey.length() : "null"));
        
        // Check environment variables directly
        System.out.println("🔧 Direct Environment Variables:");
        System.out.println("   SERPAPI_KEY: " + (System.getenv("SERPAPI_KEY") != null ? "SET" : "NOT_SET"));
        System.out.println("   RAPIDAPI_KEY: " + (System.getenv("RAPIDAPI_KEY") != null ? "SET" : "NOT_SET"));  
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
     * Debug method to test API key configuration
     */
    public CompletableFuture<String> testApiConnections() {
        return CompletableFuture.supplyAsync(() -> {
            StringBuilder results = new StringBuilder();
            
            // Test SerpAPI
            results.append("=== SerpAPI ===\n");
            if (serpApiKey.isEmpty()) {
                results.append("❌ SerpAPI key not configured\n");
            } else {
                results.append("✅ SerpAPI key configured (length: ").append(serpApiKey.length()).append("\n");
                try {
                    testSerpAPIConnection();
                    results.append("✅ SerpAPI connection successful\n");
                } catch (Exception e) {
                    results.append("❌ SerpAPI connection failed: ").append(e.getMessage()).append("\n");
                }
            }

            
            // Test RapidAPI
            results.append("=== RapidAPI ===\n");
            if (rapidApiKey.isEmpty()) {
                results.append("❌ RapidAPI key not configured\n");
            } else {
                results.append("✅ RapidAPI key configured (length: ").append(rapidApiKey.length()).append("\n");
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



    /**
     * Test SerpAPI connection with minimal request
     */
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
            System.err.println("❌ SerpAPI Error - Status: " + e.getStatusCode() + " - Message: " + e.getMessage());
            System.err.println("❌ SerpAPI Error - Response: " + e.getResponseBodyAsString());
            throw new RuntimeException("SerpAPI test failed: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
        }
        
    }


    /**
     * Test RapidAPI connection with minimal request
     */
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
            System.err.println("❌ RapidAPI Error - Status: " + e.getStatusCode() + " - Message: " + e.getMessage());
            System.err.println("❌ RapidAPI Error - Response: " + e.getResponseBodyAsString());
            throw new RuntimeException("RapidAPI test failed: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
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
    
                System.out.println("🔍 Fetching from JSearch API: " + query + " in " + location);
    
                Mono<String> response = webClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .scheme("https")
                                .host("jsearch.p.rapidapi.com")
                                .path("/search")
                                .queryParam("query", query + " " + location)
                                .queryParam("page", "1")        // safe default
                                .queryParam("num_pages", "2")   // free tier allows only 1
                                .build())
                        .header("X-RapidAPI-Key", rapidApiKey)
                        .header("X-RapidAPI-Host", "jsearch.p.rapidapi.com")
                        .retrieve()
                        .onStatus(status -> status.is4xxClientError(),
                            clientResponse -> {
                                return clientResponse.bodyToMono(String.class)
                                        .flatMap(errorBody -> {
                                            System.err.println("❌ JSearch API 4xx error: " + errorBody);
                                            return Mono.error(new RuntimeException("JSearch API 4xx error: " + errorBody));
                                        });
                            })
                        .onStatus(status -> status.is5xxServerError(),
                            clientResponse -> {
                                return clientResponse.bodyToMono(String.class)
                                        .flatMap(errorBody -> {
                                            System.err.println("❌ JSearch API 5xx error: " + errorBody);
                                            return Mono.error(new RuntimeException("JSearch API 5xx error: " + errorBody));
                                        });
                            })
                        .bodyToMono(String.class);
    
                String responseBody = response.block();
                if (responseBody != null) {
                    List<JobListing> jobs = parseJSearchResponse(responseBody);
                    if (!jobs.isEmpty()) {
                        jobListingRepository.saveAll(jobs);
                        System.out.println("✅ Saved " + jobs.size() + " jobs from JSearch");
                    } else {
                        System.out.println("❌ No jobs found from JSearch");
                        System.out.println("Response preview: " + responseBody.substring(0, Math.min(200, responseBody.length())));
                    }
                }
    
            } catch (Exception e) {
                System.err.println("❌ JSearch API error: " + e.getMessage());
            }
        });
    }
    

    /**
     * Fetch from SerpAPI Google Jobs with better error handling
     * https://serpapi.com/google-jobs-api
     */
    private CompletableFuture<Void> fetchFromSerpAPI(String query, String location) {
        return CompletableFuture.runAsync(() -> {
            try {
                if (serpApiKey.isEmpty()) {
                    System.out.println("⚠️ SerpAPI key not configured");
                    return;
                }

                System.out.println("🔍 Fetching from SerpAPI: " + query + " in " + location);

                Mono<String> response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("serpapi.com")
                        .path("/search.json") // fix: call /search instead of /search.json
                        .queryParam("engine", "google_jobs")
                        .queryParam("q", query)
                        .queryParam("location", location)
                        .queryParam("api_key", serpApiKey)
                        .queryParam("num", "10")
                        .build())
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError(),
                        clientResponse -> {
                            return clientResponse.bodyToMono(String.class)
                                    .flatMap(errorBody -> {
                                        System.err.println("❌ SerpAPI error: " + errorBody);
                                        return Mono.error(new RuntimeException("SerpAPI error: " + errorBody));
                                    });
                        })
                        .onStatus(status -> status.is5xxServerError(),
                            clientResponse -> {
                                return clientResponse.bodyToMono(String.class)
                                        .flatMap(errorBody -> {
                                            System.err.println("❌ SerpAPI error: " + errorBody);
                                            return Mono.error(new RuntimeException("SerpAPI error: " + errorBody));
                                        });
                            })
                    .bodyToMono(String.class);

                String responseBody = response.block();
                if (responseBody != null) {
                    List<JobListing> jobs = parseSerpAPIResponse(responseBody);
                    if (!jobs.isEmpty()) {
                        jobListingRepository.saveAll(jobs);
                        System.out.println("✅ Saved " + jobs.size() + " jobs from SerpAPI");
                    } else {
                        System.out.println("❌ No jobs found from SerpAPI");
                        System.out.println("Response preview: " + responseBody.substring(0, Math.min(200, responseBody.length())));
                    }
                }

            } catch (Exception e) {
                System.err.println("❌ SerpAPI error: " + e.getMessage());
                e.printStackTrace();
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
                    if (jobNode.has("job_min_salary") && !jobNode.get("job_min_salary").isNull()) {
                        job.setSalary(jobNode.get("job_min_salary").asDouble());
                    }
                    
                    // Parse posted date
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
            } else {
                System.out.println("⚠️ JSearch: No 'data' array found in response");
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
            } else {
                System.out.println("⚠️ SerpAPI: No 'jobs_results' array found in response");
            }
        } catch (Exception e) {
            System.err.println("❌ Error parsing SerpAPI response: " + e.getMessage());
            e.printStackTrace();
        }
        return jobs;
    }

    /**
     * Manual job fetch trigger with detailed logging (for testing)
     */
    public CompletableFuture<String> fetchJobsManually(String query, String location) {
        return fetchJobsFromAllSources(query, location)
                .thenApply(v -> {
                    String result = "Job fetch completed for: " + query + " in " + location;
                    System.out.println("✅ " + result);
                    return result;
                })
                .exceptionally(throwable -> {
                    String error = "Job fetch failed for: " + throwable.getMessage();
                    System.err.println("❌ " + error);
                    return error;
                });
    }
}
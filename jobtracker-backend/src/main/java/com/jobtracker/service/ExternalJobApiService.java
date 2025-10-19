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
import org.springframework.web.client.RestTemplate;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

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

    public void geocodeAndSave(JobListing job) {
        try {
            String encoded = URLEncoder.encode(job.getLocation(), StandardCharsets.UTF_8);
            String url = "https://nominatim.openstreetmap.org/search?q=" + encoded + "&format=json&limit=1";
            RestTemplate rest = new RestTemplate();
            var results = rest.getForObject(url, List.class);
            if (results != null && !results.isEmpty()) {
                @SuppressWarnings("unchecked")
                var obj = (Map<String, Object>) results.get(0);
                job.setLatitude(Double.parseDouble((String) obj.get("lat")));
                job.setLongitude(Double.parseDouble((String) obj.get("lon")));
            }
        } catch (Exception e) {
            System.err.println("Geocoding failed for: " + job.getLocation());
        }
        jobListingRepository.save(job);
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
    public CompletableFuture<Void> fetchJobsFromAllSources(
        String query, String location, String jobType,
        Double minSalary, Double maxSalary, List<String> skills) {
        System.out.println("🔍 Starting job fetch from all sources: " + query + " in " + location);
        
        return CompletableFuture.allOf(
                fetchFromJSearchAPI(query, location, jobType, minSalary, maxSalary, skills),
                fetchFromSerpAPI(query, location, jobType, minSalary, maxSalary, skills)
        );
    }

    /**
     * Fetch from JSearch (RapidAPI) - FIXED: Better location handling
     */
    private CompletableFuture<Void> fetchFromJSearchAPI(
        String query, String location, String jobType,
        Double minSalary, Double maxSalary, List<String> skills) {

        return CompletableFuture.runAsync(() -> {            
            try {
                if (rapidApiKey == null || rapidApiKey.isBlank()) {
                    System.out.println("⚠️ RapidAPI key not configured for JSearch");
                    return;
                }

                String safeQuery = (query != null && !query.isBlank()) ? query : "";
                String safeLocation = (location != null && !location.isBlank()) ? location : "";

                System.out.println("🔍 Fetching from JSearch API: " + safeQuery + " in " + safeLocation);

                StringBuilder url = new StringBuilder("https://jsearch.p.rapidapi.com/search?query=");
                StringBuilder searchTerm = new StringBuilder();
                if (safeQuery != null && !safeQuery.isBlank()) searchTerm.append(safeQuery);
                if (jobType != null && !jobType.isBlank()) {
                    if (searchTerm.length() > 0) searchTerm.append(" ");
                    searchTerm.append(jobType);
                }
                if (safeLocation != null && !safeLocation.isBlank()) {
                    if (searchTerm.length() > 0) searchTerm.append(" in ");
                    searchTerm.append(safeLocation);
                }
                if (searchTerm.length() > 0) {
                    url.append(URLEncoder.encode(searchTerm.toString(), StandardCharsets.UTF_8));
                }
                if (minSalary != null) {
                    if (searchTerm.length() > 0) searchTerm.append(" ");
                    searchTerm.append("min_salary");
                }
                if (maxSalary != null) {
                    if (searchTerm.length() > 0) searchTerm.append(" ");
                    searchTerm.append("max_salary");
                }
                if (skills != null && !skills.isEmpty()) {
                    if (searchTerm.length() > 0) searchTerm.append(" ");
                    searchTerm.append("skills");
                } 
                               
                url.append("&page=1&num_pages=1");

                System.out.println("🌍 Calling JSearch API URL → " + url);
                
                String responseBody = webClient.get()
                        .uri(url.toString())
                        .header("X-RapidAPI-Key", rapidApiKey)
                        .header("X-RapidAPI-Host", "jsearch.p.rapidapi.com")
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();

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
                e.printStackTrace();
            }
        });
    }

    /**
     * Fetch from SerpAPI Google Jobs - FIXED: Added country code support
     */
    private CompletableFuture<Void> fetchFromSerpAPI(
        String query, String location, String jobType,
        Double minSalary, Double maxSalary, List<String> skills) {
        return CompletableFuture.runAsync(() -> {
            try {
                if (serpApiKey.isEmpty()) {
                    System.out.println("⚠️ SerpAPI key not configured");
                    return;
                }

                String safeQuery = query != null && !query.isBlank() ? query.trim() : "";
                String safeLocation = location != null && !location.isBlank() ? location.trim() : "";
                System.out.println("🔍 Fetching from SerpAPI: " + safeQuery + " in " + safeLocation);

                StringBuilder url = new StringBuilder("https://serpapi.com/search.json?engine=google_jobs");

                if (!safeQuery.isBlank()) url.append("&q=").append(URLEncoder.encode(safeQuery, StandardCharsets.UTF_8));
                if (!safeLocation.isBlank()) url.append("&location=").append(URLEncoder.encode(safeLocation, StandardCharsets.UTF_8));
                url.append("&api_key=").append(serpApiKey);

                if (jobType != null && !jobType.isBlank()) url.append("&employment_type=").append(URLEncoder.encode(jobType.toLowerCase(), StandardCharsets.UTF_8));
                if (minSalary != null) url.append("&min_salary=").append(minSalary.intValue());
                if (maxSalary != null) url.append("&max_salary=").append(maxSalary.intValue());
                if (skills != null && !skills.isEmpty()) {
                    String skillsParam = skills.stream()
                            .map(s -> s.trim().replace(" ", "+"))
                            .collect(Collectors.joining(","));
                    url.append("&skills=").append(URLEncoder.encode(skillsParam, StandardCharsets.UTF_8));
                }

                System.out.println("🌍 Calling SerpAPI URL → " + url);

                String responseBody = webClient.get()
                        .uri(url.toString())
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();
                        
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
    public CompletableFuture<String> fetchJobsManually(
        String query, String location, String jobType,
        Double minSalary, Double maxSalary, List<String> skills) {
        return fetchJobsFromAllSources(query, location, jobType, minSalary, maxSalary, skills)
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
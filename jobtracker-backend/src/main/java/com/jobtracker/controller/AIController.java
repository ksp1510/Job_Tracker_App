package com.jobtracker.controller;

import com.jobtracker.config.JwtUtil;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller that proxies requests to the AI microservice.
 *
 * This controller exposes endpoints under `/ai` that forward incoming
 * requests to the FastAPI AI assistant layer.  It uses a reactive
 * {@link WebClient} to perform HTTP calls and returns the response body
 * directly to the caller.  The controller also extracts the user ID
 * from the JWT token and includes it in the payload when required.
 */
@RestController
@RequestMapping("/ai")
public class AIController {

    private final WebClient aiWebClient;
    private final JwtUtil jwtUtil;

    @Autowired
    public AIController(WebClient aiWebClient, JwtUtil jwtUtil) {
        this.aiWebClient = aiWebClient;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Proxy endpoint for job search.  Accepts a {@link JobSearchRequestDto}
     * payload and forwards it to the AI microservice's `/jobs/search` endpoint.
     *
     * @param authHeader bearer token containing the JWT
     * @param request    job search parameters
     * @return a reactive Mono wrapping the response from the AI service
     */
    @PostMapping(value = "/jobs/search", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Object>> searchJobs(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody JobSearchRequestDto request
    ) {
        // Extract user ID from JWT and insert into payload
        String token = authHeader.replace("Bearer ", "");
        String userId = jwtUtil.getUserId(token);
        Map<String, Object> payload = new HashMap<>();
        payload.put("user_id", userId);
        payload.put("query", request.getQuery());
        payload.put("location", request.getLocation());
        payload.put("skills", request.getSkills());
        payload.put("experience_level", request.getExperienceLevel());
        payload.put("salary_min", request.getSalaryMin());
        payload.put("salary_max", request.getSalaryMax());
        payload.put("remote_only", request.isRemoteOnly());
        payload.put("employment_type", request.getEmploymentType());
        payload.put("company_size", request.getCompanySize());
        payload.put("use_resume_matching", request.isUseResumeMatching());
        payload.put("resume_file_id", request.getResumeFileId());
        payload.put("limit", request.getLimit());
        return aiWebClient.post()
                .uri("/jobs/search")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Object.class)
                .map(ResponseEntity::ok);
    }

    /**
     * Proxy endpoint for analysing a resume.  Accepts a resume file ID and
     * optional job description and forwards them to `/resume/analyze`.
     *
     * @param authHeader  bearer token containing the JWT
     * @param resumeId    MongoDB ID of the resume file stored in S3
     * @param jobDescBody wrapper containing an optional job description
     * @return the analysis response as returned by the AI microservice
     */
    @PostMapping(value = "/resume/analyze/{resumeId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Object>> analyseResume(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable("resumeId") String resumeId,
            @RequestBody(required = false) JobDescriptionWrapper jobDescBody
    ) {
        String token = authHeader.replace("Bearer ", "");
        String userId = jwtUtil.getUserId(token);
        Map<String, Object> payload = new HashMap<>();
        payload.put("user_id", userId);
        payload.put("resume_file_id", resumeId);
        if (jobDescBody != null) {
            payload.put("job_description", jobDescBody.getJobDescription());
        }
        return aiWebClient.post()
                .uri("/resume/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Object.class)
                .map(ResponseEntity::ok);
    }

    /**
     * Proxy endpoint for resume optimisation.  Accepts a resume text and job
     * description and forwards them to `/resume/optimize`.
     *
     * @param authHeader bearer token containing the JWT
     * @param request    optimisation parameters
     * @return the optimisation response
     */
    @PostMapping(value = "/resume/optimize", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Object>> optimiseResume(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody ResumeOptimiseRequestDto request
    ) {
        String token = authHeader.replace("Bearer ", "");
        String userId = jwtUtil.getUserId(token);
        Map<String, Object> payload = new HashMap<>();
        payload.put("user_id", userId);
        payload.put("resume_text", request.getResumeText());
        payload.put("job_description", request.getJobDescription());
        payload.put("optimization_focus", request.getOptimizationFocus());
        payload.put("preserve_format", request.isPreserveFormat());
        return aiWebClient.post()
                .uri("/resume/optimize")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Object.class)
                .map(ResponseEntity::ok);
    }

    /**
     * Proxy endpoint for generating interview questions.  Forwards the
     * request to `/interview/questions` on the AI microservice.
     *
     * @param authHeader bearer token containing the JWT
     * @param request    request parameters
     * @return the question list generated by the AI microservice
     */
    @PostMapping(value = "/interview/questions", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Object>> interviewQuestions(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody InterviewQuestionsDto request
    ) {
        String token = authHeader.replace("Bearer ", "");
        String userId = jwtUtil.getUserId(token);
        Map<String, Object> payload = new HashMap<>();
        payload.put("user_id", userId);
        payload.put("resume_text", request.getResumeText());
        payload.put("resume_file_id", request.getResumeFileId());
        payload.put("job_description", request.getJobDescription());
        payload.put("job_title", request.getJobTitle());
        payload.put("company_name", request.getCompanyName());
        payload.put("interview_type", request.getInterviewType());
        payload.put("difficulty_level", request.getDifficultyLevel());
        payload.put("num_questions", request.getNumQuestions());
        return aiWebClient.post()
                .uri("/interview/questions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Object.class)
                .map(ResponseEntity::ok);
    }

    /**
     * Proxy endpoint to start a mock interview session.  Sends the request to
     * `/interview/mock/start` on the AI microservice and returns the session ID
     * and first question.
     *
     * @param authHeader bearer token containing the JWT
     * @param request    request parameters
     * @return the session details as returned by the AI microservice
     */
    @PostMapping(value = "/interview/mock/start", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Object>> startMockInterview(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody MockInterviewRequestDto request
    ) {
        String token = authHeader.replace("Bearer ", "");
        String userId = jwtUtil.getUserId(token);
        Map<String, Object> payload = new HashMap<>();
        payload.put("user_id", userId);
        payload.put("resume_text", request.getResumeText());
        payload.put("resume_file_id", request.getResumeFileId());
        payload.put("job_description", request.getJobDescription());
        payload.put("job_title", request.getJobTitle());
        payload.put("company_name", request.getCompanyName());
        payload.put("interview_duration", request.getInterviewDuration());
        payload.put("interview_type", request.getInterviewType());
        return aiWebClient.post()
                .uri("/interview/mock/start")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Object.class)
                .map(ResponseEntity::ok);
    }

    /**
     * Proxy endpoint to submit an answer during a mock interview.  Sends the
     * request to `/interview/mock/answer` on the AI microservice and returns
     * feedback along with the next question.
     *
     * @param request the answer payload
     * @return updated session state from the AI microservice
     */
    @PostMapping(value = "/interview/mock/answer", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Object>> answerMockInterview(
            @RequestBody MockInterviewAnswerDto request
    ) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("session_id", request.getSessionId());
        payload.put("question_id", request.getQuestionId());
        payload.put("answer", request.getAnswer());
        payload.put("time_taken", request.getTimeTaken());
        return aiWebClient.post()
                .uri("/interview/mock/answer")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Object.class)
                .map(ResponseEntity::ok);
    }

    // --- DTOs ---
    @Data
    public static class JobSearchRequestDto {
        private String query;
        private String location;
        private java.util.List<String> skills;
        private String experienceLevel;
        private Integer salaryMin;
        private Integer salaryMax;
        private boolean remoteOnly;
        private String employmentType;
        private String companySize;
        private boolean useResumeMatching = true;
        private String resumeFileId;
        private int limit = 20;
    }

    @Data
    public static class JobDescriptionWrapper {
        private String jobDescription;
    }

    @Data
    public static class ResumeOptimiseRequestDto {
        private String resumeText;
        private String jobDescription;
        private java.util.List<String> optimizationFocus;
        private boolean preserveFormat = true;
    }

    @Data
    public static class InterviewQuestionsDto {
        private String resumeText;
        private String resumeFileId;
        private String jobDescription;
        private String jobTitle;
        private String companyName;
        private String interviewType = "mixed";
        private String difficultyLevel = "medium";
        private int numQuestions = 10;
    }

    @Data
    public static class MockInterviewRequestDto {
        private String resumeText;
        private String resumeFileId;
        private String jobDescription;
        private String jobTitle;
        private String companyName;
        private int interviewDuration = 30;
        private String interviewType = "mixed";
    }

    @Data
    public static class MockInterviewAnswerDto {
        private String sessionId;
        private String questionId;
        private String answer;
        private Integer timeTaken;
    }
}
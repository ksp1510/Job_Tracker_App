package com.jobtracker.controller;

import com.jobtracker.model.Application;
import com.jobtracker.model.Files;
import com.jobtracker.repository.FileRepository;
import com.jobtracker.service.ApplicationService;
import com.jobtracker.service.InputValidationService;

import com.jobtracker.model.Status;

import com.jobtracker.exception.ResourceNotFoundException;

import lombok.AllArgsConstructor;
import lombok.Data;

import com.jobtracker.config.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/applications")
public class ApplicationController {

    private final ApplicationService service;
    private final JwtUtil jwtUtil;
    private final FileRepository fileRepository;
    private final InputValidationService inputValidationService;

    public ApplicationController(ApplicationService service, JwtUtil jwtUtil,
                                FileRepository fileRepository, InputValidationService inputValidationService) {
        this.service = service;
        this.jwtUtil = jwtUtil;
        this.fileRepository = fileRepository;
        this.inputValidationService = inputValidationService;
    }

    // 🔹 Create new application
    @PostMapping
    public ResponseEntity<Application> create(@Valid @RequestBody Application application,
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String userId = jwtUtil.getUserId(token);

        // SECURITY: Validate input to prevent NoSQL injection
        application.setCompanyName(inputValidationService.sanitizeJobField(application.getCompanyName()));
        application.setJobTitle(inputValidationService.sanitizeJobField(application.getJobTitle()));
        if (application.getJobLocation() != null) {
            application.setJobLocation(inputValidationService.sanitizeJobField(application.getJobLocation()));
        }
        if (application.getStatus() != null) {
        String[] allowedStatuses = {"APPLIED", "INTERVIEW", "ASSESSMENT", "OFFER", "REJECTED", "HIRED", "WITHDRAWN"};
        String validated = inputValidationService.validateStatus(application.getStatus(), allowedStatuses);
        application.setStatus(Status.valueOf(validated)); // convert validated string back to enum
    }


        application.setUserId(userId);
        return ResponseEntity.ok(service.createApplication(application));
    }

    // 🔹 Get all applications for logged in user
    @GetMapping
    public ResponseEntity<List<Application>> getAll(
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String userId = jwtUtil.getUserId(token);
        return ResponseEntity.ok(service.getUserApplications(userId));
    }

    // 🔹 Get all applications by status for logged in user
    @GetMapping("/by-status")
    public ResponseEntity<List<Application>> getAllByStatus(
        @RequestParam String status,
        @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String userId = jwtUtil.getUserId(token);

        // SECURITY: Validate status parameter to prevent injection
        String[] allowedStatuses = {"APPLIED", "INTERVIEW", "ASSESSMENT", "OFFER", "REJECTED", "HIRED", "WITHDRAWN"};
        String validatedStatus = inputValidationService.validateStatus(status, allowedStatuses);

        return ResponseEntity.ok(service.findByUserIdAndStatus(userId, validatedStatus));
    }

    // 🔹 Get application by ID - FIXED: Returns Application instead of Optional<Application>
    @GetMapping("/{id}")
    public ResponseEntity<Application> getById(@PathVariable String id,
                                            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String userId = jwtUtil.getUserId(token);

        Application app = service.getApplication(id, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        
        return ResponseEntity.ok(app);
    }

    // 🔹 Update application
    @PutMapping("/{id}")
    public ResponseEntity<Application> update(@Valid @RequestBody Application appDetails,
            @PathVariable String id,
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String userId = jwtUtil.getUserId(token);
        return ResponseEntity.ok(service.updateApplication(id, userId, appDetails));
    }

    // 🔹 Delete application
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id,
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String userId = jwtUtil.getUserId(token);
        service.deleteApplication(id, userId);
        return ResponseEntity.noContent().build();
    }

    // 🔹 Get application with files - FIXED: Proper constructor implementation
    @GetMapping("/{id}/with-files")
    public ResponseEntity<ApplicationResponse> getApplicationWithFiles(
            @PathVariable String id,
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String userId = jwtUtil.getUserId(token);
        
        Application app = service.getApplication(id, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        
        List<Files> files = fileRepository.findByApplicationId(id);

        return ResponseEntity.ok(new ApplicationResponse(app, files));
    }

    @Data
    @AllArgsConstructor
    static class ApplicationResponse {
        private Application application;
        private List<Files> files;
    }
}
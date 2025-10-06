package com.jobtracker.controller;

import com.jobtracker.model.Application;
import com.jobtracker.model.Files;
import com.jobtracker.repository.FileRepository;
import com.jobtracker.service.ApplicationService;
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

    public ApplicationController(ApplicationService service, JwtUtil jwtUtil, FileRepository fileRepository) {
        this.service = service;
        this.jwtUtil = jwtUtil;
        this.fileRepository = fileRepository;
    }

    // 🔹 Create new application
    @PostMapping
    public ResponseEntity<Application> create(@Valid @RequestBody Application application,
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String userId = jwtUtil.getUserId(token);
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
        return ResponseEntity.ok(service.findByUserIdAndStatus(userId, status));
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
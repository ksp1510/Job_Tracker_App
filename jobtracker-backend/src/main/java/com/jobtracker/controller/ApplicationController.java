package com.jobtracker.controller;

import com.jobtracker.model.Application;
import com.jobtracker.model.Files;
import com.jobtracker.repository.FileRepository;
import com.jobtracker.service.ApplicationService;

import lombok.AllArgsConstructor;
import lombok.Data;

import com.jobtracker.config.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

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
        return ResponseEntity.ok(service.createApplication(application));
    }

    // 🔹 Get all applications for logged in user
    @GetMapping
    public ResponseEntity<Object> getAll(
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String userId = jwtUtil.getUserId(token);
        return ResponseEntity.ok(service.getUserApplications(userId));
    }

    // 🔹 Get all applications by status for logged in user
    @GetMapping("/by-status")
    public ResponseEntity<Object> getAllByStatus(
        @RequestParam String status,
        @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String userId = jwtUtil.getUserId(token);
        return ResponseEntity.ok(service.findByUserIdAndStatus(userId, status));
    }

    // 🔹 Get application by ID
    @GetMapping("/{id}")
    public ResponseEntity<Optional<Application>> getById(@PathVariable String id,
                                            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String userId = jwtUtil.getUserId(token);

        return ResponseEntity.ok(service.getApplication(id, userId));
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
    public ResponseEntity<Void> delete(@Valid 
            @PathVariable String id,
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String userId = jwtUtil.getUserId(token);
        service.deleteApplication(id, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/with-files")
    public ResponseEntity<ApplicationResponse> getApplicationWithFiles(@Valid 
            @PathVariable String id,
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String userId = jwtUtil.getUserId(token);
        Optional<Application> app = service.getApplication(id, userId);

        List<Files> files = fileRepository.findByApplicationId(id);

        return ResponseEntity.ok(new ApplicationResponse(app, files));
    }


    @Data
    @AllArgsConstructor
    static class ApplicationResponse {
        public ApplicationResponse(Optional<Application> app, List<Files> files2) {
            //TODO Auto-generated constructor stub
        }
        private Application application;
        private List<Files> files;
    }
}

package com.jobtracker.controller;

import com.jobtracker.model.Application;
import com.jobtracker.service.ApplicationService;
import com.jobtracker.config.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/applications")
public class ApplicationController {

    private final ApplicationService service;
    private final JwtUtil jwtUtil;

    public ApplicationController(ApplicationService service, JwtUtil jwtUtil) {
        this.service = service;
        this.jwtUtil = jwtUtil;
    }

    // 🔹 Create new application
    @PostMapping
    public ResponseEntity<Application> create(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Application application) {
        String token = authHeader.replace("Bearer ", "");
        String userId = jwtUtil.getUserId(token);
        return ResponseEntity.ok(service.save(userId, application));
    }

    // 🔹 Get all applications for logged in user
    @GetMapping
    public ResponseEntity<List<Application>> getAll(
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String userId = jwtUtil.getUserId(token);
        return ResponseEntity.ok(service.getAllByUser(userId));
    }

    // 🔹 Get application by ID
    @GetMapping("/{id}")
    public ResponseEntity<Application> getById(@PathVariable String id) {
        return service.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 🔹 Update application
    @PutMapping("/{id}")
    public ResponseEntity<Application> update(
            @PathVariable String id,
            @RequestBody Application appDetails) {
        return ResponseEntity.ok(service.update(id, appDetails));
    }

    // 🔹 Delete application
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

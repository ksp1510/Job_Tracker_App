package com.jobtracker.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/test")
public class ApiKeyTestController {
    
    @Value("${app.api.serpapi.key:NOT_SET}")
    private String serpApiKey;
    
    @Value("${app.api.rapidapi.key:NOT_SET}")
    private String rapidApiKey;
    
    @Value("${app.api.theirstack.key:NOT_SET}")
    private String theirStackKey;
    
    @GetMapping("/api-keys-status")
    public ResponseEntity<Map<String, String>> checkApiKeysStatus() {
        Map<String, String> status = new HashMap<>();
        status.put("serpapi", serpApiKey.isEmpty() ? "NOT_SET" : "SET");
        status.put("rapidapi", rapidApiKey.isEmpty() ? "NOT_SET" : "SET");
        status.put("theirstack", theirStackKey.isEmpty() ? "NOT_SET" : "SET");
        
        // Never return actual keys, only status
        return ResponseEntity.ok(status);
    }
}

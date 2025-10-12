package com.jobtracker.controller;

import com.jobtracker.service.SesService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/test")
public class TestController {

    private final SesService sesService;

    public TestController(SesService sesService) {
        this.sesService = sesService;
    }

    @PostMapping("/send-test-email")
    public ResponseEntity<Map<String, String>> sendTestEmail(
            @RequestParam String toEmail,
            @RequestHeader("Authorization") String authHeader) {
        
        Map<String, String> response = new HashMap<>();
        
        try {
            String subject = "JobTracker Test Email";
            String htmlBody = "<html><body>" +
                    "<h1>Test Email from JobTracker</h1>" +
                    "<p>If you're seeing this, email notifications are working correctly!</p>" +
                    "<p>Time: " + java.time.LocalDateTime.now() + "</p>" +
                    "</body></html>";
            
            sesService.sendHtmlEmail(toEmail, subject, htmlBody);
            
            response.put("status", "success");
            response.put("message", "Test email sent to " + toEmail);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}
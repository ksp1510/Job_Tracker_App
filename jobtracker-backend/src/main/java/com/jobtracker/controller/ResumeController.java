package com.jobtracker.controller;

import com.jobtracker.dto.ResumeParseResponse;
import com.jobtracker.service.ResumePipelineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumePipelineService resumePipelineService;

    @PostMapping("/parse/{fileId}")
    public ResponseEntity<ResumeParseResponse> parse(
            @PathVariable String fileId,
            Authentication authentication
    ) {
        String userId = authentication.getName();
        return ResponseEntity.ok(resumePipelineService.parseResume(fileId, userId));
    }

    @GetMapping("/parsed/{fileId}")
    public ResponseEntity<ResumeParseResponse> latest(
            @PathVariable String fileId,
            Authentication authentication
    ) {
        String userId = authentication.getName();
        return ResponseEntity.ok(resumePipelineService.getLatestParsed(fileId, userId));
    }
}

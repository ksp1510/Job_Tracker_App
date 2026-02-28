package com.jobtracker.controller;

import com.jobtracker.dto.resume.ResumeParseResponse;
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

    @GetMapping("/parsed/v2/{fileId}")
    public ResponseEntity<?> latestV2(@PathVariable String fileId, Authentication authentication) {
        String userId = authentication.getName();
        return ResponseEntity.ok(resumePipelineService.getLatestParsedV2(fileId, userId));
    }

    @PostMapping("/parse/v2/{fileId}")
    public ResponseEntity<?> parseV2(@PathVariable String fileId, Authentication authentication) {
        String userId = authentication.getName();
        return ResponseEntity.ok(resumePipelineService.parseResumeV2(fileId, userId));
    }

}

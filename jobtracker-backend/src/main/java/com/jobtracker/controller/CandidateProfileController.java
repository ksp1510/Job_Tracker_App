package com.jobtracker.controller;

import com.jobtracker.model.CandidateProfile;
import com.jobtracker.service.CandidateProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/candidates/profile")
@RequiredArgsConstructor
public class CandidateProfileController {

    private final CandidateProfileService candidateProfileService;

    @PostMapping("/from-resume/v2/{fileId}")
    public ResponseEntity<CandidateProfile> buildFromResumeV2(@PathVariable String fileId, Authentication auth) {
        String userId = auth.getName();
        return ResponseEntity.ok(candidateProfileService.buildFromLatestParsedV2(userId, fileId));
    }

    @GetMapping("/latest")
    public ResponseEntity<CandidateProfile> latest(Authentication auth) {
        String userId = auth.getName();
        return ResponseEntity.ok(candidateProfileService.getLatest(userId));
    }
}

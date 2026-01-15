package com.jobtracker.controller;

import com.jobtracker.dto.jobmatch.JobMatchResult;
import com.jobtracker.model.CandidateProfile;
import com.jobtracker.service.CandidateProfileService;
import com.jobtracker.service.JobMatchingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/matches")
@RequiredArgsConstructor
public class JobMatchController {

    private final CandidateProfileService candidateProfileService;
    private final JobMatchingService jobMatchingService;

    @GetMapping("/jobs")
    public ResponseEntity<List<JobMatchResult>> matchJobs(
            @RequestParam(defaultValue = "20") int limit,
            Authentication auth
    ) {
        String userId = auth.getName();
        CandidateProfile profile = candidateProfileService.getLatest(userId);
        return ResponseEntity.ok(jobMatchingService.matchTopJobs(profile, limit));
    }
}

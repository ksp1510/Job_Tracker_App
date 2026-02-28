package com.jobtracker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobtracker.dto.resume.ResumeStructuredData;
import com.jobtracker.exception.ResourceNotFoundException;
import com.jobtracker.model.CandidateProfile;
import com.jobtracker.model.ParsedResume;
import com.jobtracker.repository.CandidateProfileRepository;
import com.jobtracker.repository.ParsedResumeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CandidateProfileService {

    private final ParsedResumeRepository parsedResumeRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final CandidateProfileBuilderService builder;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CandidateProfile buildFromLatestParsedV2(String userId, String fileId) {
        ParsedResume latestV2 = parsedResumeRepository
                .findTopByUserIdAndFileIdAndParserVersionOrderByParsedAtDesc(userId, fileId, "v2")
                .orElseThrow(() -> new ResourceNotFoundException("No parsed resume found (v2) for this file"));

        ResumeStructuredData data;
        try {
            data = objectMapper.readValue(latestV2.getParsedJson(), ResumeStructuredData.class);
        } catch (Exception e) {
            throw new IllegalStateException("Stored parsedJson is not valid ResumeStructuredData: " + e.getMessage(), e);
        }

        CandidateProfile profile = builder.build(userId, fileId, latestV2, data);
        return candidateProfileRepository.save(profile);
    }

    public CandidateProfile getLatest(String userId) {
        return candidateProfileRepository
                .findTopByUserIdOrderByGeneratedAtDesc(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No candidate profile found"));
    }
}

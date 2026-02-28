package com.jobtracker.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data
@Document(collection = "candidate_profiles")
public class CandidateProfile {

    @Id
    private String id;

    private String userId;

    // Source pointers
    private String resumeFileId;       // Files._id
    private String parsedResumeId;     // ParsedResume._id (optional)
    private String parserVersion;      // "v2"
    private Instant generatedAt;

    // Core fields for matching/autofill
    private String fullName;
    private String email;
    private String phone;
    private String city;
    private String region;
    private String country;

    private String summary;

    // Normalized lists (lowercase-normalized stored, but keep original too if you want)
    private List<String> titles;
    private List<String> skills;         // flattened from skills.primary/secondary/tools/languages
    private List<String> technologies;   // from experience/projects
    private Integer totalExperienceMonths;

    // Optional: keep original structured JSON for debugging
    private String structuredJson;
}

package com.jobtracker.dto.jobmatch;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class JobMatchResult {
    private String jobId;
    private String title;
    private String company;
    private String location;
    private boolean remote;

    private double score;                 // 0..1
    private List<String> matchedSkills;
    private List<String> missingSkills;
    private List<String> matchedKeywords;
}

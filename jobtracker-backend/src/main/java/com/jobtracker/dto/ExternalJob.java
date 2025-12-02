// Job API Response DTOs (for external API integration)
package com.jobtracker.dto;

import lombok.Data;
import java.util.List;

@Data
public class ExternalJob {
    private String jobId;
    private String jobTitle;
    private String companyName;
    private String location;
    private String jobType;
    private String experienceLevel;
    private String description;
    private String applyUrl;
    private Double salaryMin;
    private Double salaryMax;
    private List<String> requiredSkills;
    private String postedDate;
    private String source;
}
// Job Listing Model (for external job data)
package com.jobtracker.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.List;
@Data
@Document(collection = "job_listings")
public class JobListing {
    @Id
    private String id;
    private String externalId; // ID from job board API
    private String title;
    private String company;
    private String location;
    private String description;
    private String jobType;
    private String experienceLevel;
    private Double salary;
    private String salaryRange;
    private List<String> skills;
    private String applyUrl;
    private String source; // LINKEDIN, INDEED, GLASSDOOR, etc.
    private Instant postedDate;
    private Instant fetchedAt = Instant.now();
    private boolean isActive = true;
}
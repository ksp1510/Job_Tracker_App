// Job Search Model
package com.jobtracker.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.List;

@Data
@Document(collection = "job_searches")
public class JobSearch {
    @Id
    private String id;
    private String userId;
    private String query;
    private String location;
    private String jobType; // FULL_TIME, PART_TIME, CONTRACT, INTERNSHIP
    private String experienceLevel; // ENTRY, MID, SENIOR
    private Double minSalary;
    private Double maxSalary;
    private List<String> skills;
    private String companySize; // STARTUP, SMALL, MEDIUM, LARGE
    private boolean remoteOnly;
    private Instant searchedAt = Instant.now();
    private int resultsCount;
}
// Job Listing Model (for external job data)
package com.jobtracker.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.List;
@Data
@Document(collection = "job_listings")
@CompoundIndex(name = "active_postedDate_idx", 
                def = "{'isActive': 1, 'postedDate': -1}")
@CompoundIndex(name = "title_location_idx", 
                def = "{'title': 1, 'location': 1}")
public class JobListing {
    @Id
    private String id;

    @Indexed
    private String externalId; // ID from job board API
    private String title;
    private String company;
    private String location;

    private double latitude;
    private double longitude;

    private boolean remote;
    private String description;
    private String jobType;
    private String experienceLevel;
    private Double salary;
    private String salaryRange;
    private List<String> skills;
    private String applyUrl;
    private String source; // LINKEDIN, INDEED, GLASSDOOR, etc.
    
    @Indexed
    private Instant postedDate;
    private Instant fetchedAt = Instant.now();
    
    @Indexed
    private boolean isActive = true;
}
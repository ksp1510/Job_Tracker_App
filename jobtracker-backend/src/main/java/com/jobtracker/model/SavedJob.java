// Saved Job Model (user's saved/bookmarked jobs)
package com.jobtracker.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Data
@Document(collection = "saved_jobs")
public class SavedJob {
    @Id
    private String id;
    private String userId;
    private String jobListingId;
    private String notes;
    private Instant savedAt = Instant.now();
    private boolean applied = false;
    private String applicationId; // Link to Application if user applies
}
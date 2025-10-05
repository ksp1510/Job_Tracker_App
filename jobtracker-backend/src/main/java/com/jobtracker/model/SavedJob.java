// Saved Job Model (user's saved/bookmarked jobs)
package com.jobtracker.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Data
@Document(collection = "saved_jobs")
public class SavedJob {
    @Id
    private String id;

    @Indexed
    private String userId;

    @Indexed
    private String jobListingId;
    private String notes;
    private LocalDateTime savedAt;
    private boolean applied = false;
    private String applicationId; // Link to Application if user applies
}
package com.jobtracker.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Document(collection = "files")
public class Files {

    @Id
    private String id;

    private String userId;

    private String applicationId;

    private String fileName;

    private String type; // Resume, Cover Letter

    private String filePath;   // S3 bucket link
    private Instant uploadedAt = Instant.now();

    private String notes; // e.g. "Tailored for backend jobs"
}

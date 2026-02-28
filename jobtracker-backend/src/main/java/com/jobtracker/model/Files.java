package com.jobtracker.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Data
@Document(collection = "files")
public class Files {

    @Id
    private String id;

    private String userId;

    /**
     * Null for library files; set for application-tied files.
     */
    private String applicationId;

    /**
     * S3 object key ONLY (single source of truth).
     * Example:
     *  First_Last_auth0|xxxx/Resumes/First_Last_Resumes_v1_123.pdf
     */
    private String s3Key;

    private String fileName;

    /**
     * "Resume" or "CoverLetter"
     */
    private String type;

    private Instant uploadedAt = Instant.now();

    private String notes;

    /**
     * True => library (reusable), False => application-scoped
     */
    private Boolean reusable;

    /**
     * Version for library files per (userId, type).
     */
    private Integer version;

    /**
     * Reserved for parsing output.
     */
    private Map<String, Object> parsedData;
}

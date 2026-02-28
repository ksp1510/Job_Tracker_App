package com.jobtracker.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Document(collection = "parsed_resumes")
public class ParsedResume {

    @Id
    private String id;

    private String userId;
    private String fileId;

    private String parserVersion;     // e.g. "v1"
    private String model;             // e.g. "gpt-5-mini" (optional for now)
    private Instant parsedAt;

    private String status;            // "SUCCESS" | "FAILED"
    private String errorMessage;      // populated if FAILED

    private String rawText;           // optional (can be large)
    private String parsedJson;        // store JSON as string to keep it flexible
}

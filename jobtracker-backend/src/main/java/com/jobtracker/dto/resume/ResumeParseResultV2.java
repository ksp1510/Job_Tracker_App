package com.jobtracker.dto.resume;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class ResumeParseResultV2 {
    private String fileId;
    private String parserVersion; // "v2"
    private String status;        // SUCCESS | FAILED
    private Instant parsedAt;

    private ResumeStructuredData data; // structured JSON -> object
    private String rawTextPreview;     // keep for debugging
    private String error;             // optional
}

package com.jobtracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class ResumeParseResponse {

    // This is v1 of the parser
    private String fileId;
    private String parserVersion;
    private String status;       // "SUCCESS" | "FAILED"
    private Instant parsedAt;
    private String parsedJson;   // for now return as string
}

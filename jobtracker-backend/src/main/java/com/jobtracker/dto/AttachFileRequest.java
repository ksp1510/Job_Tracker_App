package com.jobtracker.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AttachFileRequest {

    @NotBlank
    private String applicationId;

    @NotBlank
    private String fileId;

    /**
     * Must be "Resume" or "CoverLetter" (match your existing values).
     */
    @NotBlank
    private String type;
}

// Job API Response DTOs (for external API integration)
package com.jobtracker.dto;

import lombok.Data;
import java.util.List;

@Data
public class ExternalJobResponse {
    private List<ExternalJob> data;
    private String status;
    private int totalJobs;
    private String message;
}
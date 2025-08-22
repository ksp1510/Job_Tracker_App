package com.jobtracker.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

@Data
@Document(collection = "applications")
public class Application {

    @Id
    private String id;

    @NotBlank(message = "User ID is required")
    private String userId;          // reference to User
    
    @NotBlank(message = "Company Name is required")
    private String companyName;
    
    @NotBlank(message = "Job Title is required")
    private String jobTitle;
    
    @NotBlank(message = "Job Description is required")
    private String jobDescription;
    
    @DecimalMin(value = "0.0", inclusive = true, message = "Salary must be non-negative")
    private Double salary;
    private String jobLink;
    private String recruiterContact;

    @NotBlank(message = "Status is required")
    @Pattern(
        regexp = "APPLIED|INTERVIEW|OFFER|REJECTED|HIRED",
        message = "Status must be one of: APPLIED, INTERVIEW, OFFER, REJECTED, HIRED"
    )
    private Status status;          // Applied, Interview, Offer, Rejected
    private LocalDate appliedDate;
    private LocalDate lastFollowUpDate;
    
    private String resumeId;        // reference to resume used
    private String coverLetterId;
    private String notes;
    private String referral;
}



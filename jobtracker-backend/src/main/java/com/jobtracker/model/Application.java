package com.jobtracker.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.InstantDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;
import com.jobtracker.util.SalaryDeserializer;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
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

    @NotBlank(message = "Job Location is required")
    private String jobLocation;
    
    @NotBlank(message = "Job Description is required")
    private String jobDescription;
    
    @JsonAlias({"salary"})
    @JsonDeserialize(using = SalaryDeserializer.class)
    @DecimalMin(value = "0.0", inclusive = true)
    private Double salary;

    private String salaryText; // optional field for raw value

    private String jobLink;
    private String recruiterContact;

    @NotNull(message = "Status is required")
    private Status status;          // Applied, Interview, Offer, Rejected
    private Instant appliedDate;
    private Instant lastStatusChangeDate;

    @Indexed
    private String externalJobId;

    private String interviewDate;
    private String assessmentDeadline;
    
    private String resumeId;        // reference to resume used
    private String coverLetterId;
    private String notes;
    private String referral;

    
    private Instant createdAt;
    private Instant updatedAt;

    // Add constructor for easy testing
    public Application() {}
    
    public Application(String companyName, String jobTitle, String jobLocation, 
                      String jobDescription, Status status) {
        this.companyName = companyName;
        this.jobTitle = jobTitle;
        this.jobLocation = jobLocation;
        this.jobDescription = jobDescription;
        this.status = status;
        this.appliedDate = Instant.now();
        
    }
}



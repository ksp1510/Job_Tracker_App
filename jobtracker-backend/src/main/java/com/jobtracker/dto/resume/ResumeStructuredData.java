package com.jobtracker.dto.resume;

import lombok.Data;
import java.util.List;

@Data
public class ResumeStructuredData {

    private Basics basics;
    private String summary;
    private Skills skills;
    private List<Experience> experience;
    private List<Project> projects;
    private List<Education> education;
    private List<Certification> certifications;
    private Metadata metadata;

    @Data
    public static class Basics {
        private String fullName;
        private String headline;
        private String email;
        private String phone;
        private Location location;
        private List<Link> links;
    }

    @Data
    public static class Location {
        private String city;
        private String region;
        private String country;
    }

    @Data
    public static class Link {
        private String label;
        private String url;
    }

    @Data
    public static class Skills {
        private List<String> primary;
        private List<String> secondary;
        private List<String> tools;
        private List<String> languages;
    }

    @Data
    public static class Experience {
        private String company;
        private String location;
        private String title;
        private String employmentType;
        private String startDate; // YYYY-MM
        private String endDate;   // YYYY-MM or PRESENT
        private List<String> highlights;
        private List<String> technologies;
    }

    @Data
    public static class Project {
        private String name;
        private String description;
        private List<String> highlights;
        private List<String> technologies;
        private List<String> links;
    }

    @Data
    public static class Education {
        private String institution;
        private String location;
        private String degree;
        private String fieldOfStudy;
        private String gpa;
        private String startDate; // YYYY-MM
        private String endDate;   // YYYY-MM
    }

    @Data
    public static class Certification {
        private String name;
        private String issuer;
        private String issuedDate;   // YYYY-MM
        private String expiryDate;   // YYYY-MM or null
        private String credentialUrl;
    }

    @Data
    public static class Metadata {
        private String source;        // LLM
        private Double confidence;    // 0..1
        private List<String> warnings;
    }
}

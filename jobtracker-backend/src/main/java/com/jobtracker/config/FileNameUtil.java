package com.jobtracker.config;

public class FileNameUtil {

    public static String safe(String s) {
        return (s == null || s.isBlank()) ? "NA" : s.replaceAll("\\s+", "_");
    }
    
    public static String generate(String firstName, String lastName, String jobTitle, String company, boolean timestamp) {
        StringBuilder sb = new StringBuilder();
        sb.append(safe(firstName)).append("_")
          .append(safe(lastName)).append("_")
          .append(safe(jobTitle));
        if (company != null && !company.isBlank()) {
            sb.append("_").append(company.replaceAll("\\s+", "_"));
        }
        if (timestamp) {
            sb.append("_").append(System.currentTimeMillis());
        }
        return sb.toString();
    }
}

    

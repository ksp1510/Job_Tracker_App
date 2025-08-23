package com.jobtracker.config;

public class FileNameUtil {

    public static String generate(String firstName, String lastName, String jobTitle, String company, boolean timestamp) {
        StringBuilder sb = new StringBuilder();
        sb.append(firstName.replaceAll("\\s+", "_"));
        sb.append("_").append(lastName.replaceAll("\\s+", "_"));
        sb.append("_").append(jobTitle.replaceAll("\\s+", "_"));
        if (company != null && !company.isBlank()) {
            sb.append("_").append(company.replaceAll("\\s+", "_"));
        }
        if (timestamp) {
            sb.append("_").append(System.currentTimeMillis());
        }
        return sb.toString();
    }
}

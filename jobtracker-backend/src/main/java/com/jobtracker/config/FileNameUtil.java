package com.jobtracker.config;

public class FileNameUtil {
    public static String generate(String first, String last, String job, String company, boolean duplicate) {
        String base = first + last + job;
        return duplicate ? base + company : base;
    }
    
}

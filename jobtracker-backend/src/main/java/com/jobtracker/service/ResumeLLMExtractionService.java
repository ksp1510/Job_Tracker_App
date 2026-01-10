package com.jobtracker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobtracker.dto.resume.ResumeStructuredData;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class ResumeLLMExtractionService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OpenAIService openAIService; // you will implement as a thin HTTP client

    @Value("${openai.enabled:false}")
    private boolean openAiEnabled;

    public ResumeStructuredData extract(String rawText) {
        if (!openAiEnabled) {
            ResumeStructuredData data = new ResumeStructuredData();
            data.setSummary("openai.enabled=false; returning fallback structure");
            ResumeStructuredData.Basics basics = new ResumeStructuredData.Basics();
            data.setBasics(basics);
            return data;
        }


        String responseJson = openAIService.extractStructuredResume(rawText); // must return JSON matching ResumeStructuredData
        try {
            return objectMapper.readValue(responseJson, ResumeStructuredData.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to map LLM JSON to ResumeStructuredData: " + e.getMessage(), e);
        }
    }
}

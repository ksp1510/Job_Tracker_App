package com.jobtracker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;

@Service
public class ResumeParsingService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Stub parser: returns a minimal JSON with extracted text.
     * Replace later with LLM-based schema extraction.
     */
    public String parseToJson(String rawText) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("parser", "stub-v1");
        root.put("rawTextLength", rawText == null ? 0 : rawText.length());
        root.put("rawTextPreview", rawText == null ? "" : rawText.substring(0, Math.min(rawText.length(), 800)));
        return root.toString();
    }
}

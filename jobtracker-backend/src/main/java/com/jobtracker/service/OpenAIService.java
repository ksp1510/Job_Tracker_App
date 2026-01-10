package com.jobtracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
@RequiredArgsConstructor
public class OpenAIService {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${openai.api.key:}")
    private String apiKey;

    @Value("${openai.model:gpt-5-mini}")
    private String model;

    @Value("${openai.base-url:https://api.openai.com}")
    private String baseUrl;

    public String extractStructuredResume(String rawText) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Missing OpenAI API key (openai.api.key / OPENAI_API_KEY).");
        }

        // Build request JSON safely
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.put("temperature", 1);

        ArrayNode messages = body.putArray("messages");

        ObjectNode sys = objectMapper.createObjectNode();
        sys.put("role", "system");
        sys.put("content",
                "You are a resume parsing engine.\n"
            + "Output ONLY valid JSON that matches exactly this schema (same keys, same nesting).\n"
            + "Do not output markdown, backticks, comments, or extra keys.\n\n"

            + "Schema:\n"
            + "{\n"
            + "  \"basics\": {\n"
            + "    \"fullName\": string|null,\n"
            + "    \"headline\": string|null,\n"
            + "    \"email\": string|null,\n"
            + "    \"phone\": string|null,\n"
            + "    \"location\": {\"city\": string|null, \"region\": string|null, \"country\": string|null},\n"
            + "    \"links\": [{\"label\": string|null, \"url\": string|null}]\n"
            + "  },\n"
            + "  \"summary\": string|null,\n"
            + "  \"skills\": {\"primary\": [string], \"secondary\": [string], \"tools\": [string], \"languages\": [string]},\n"
            + "  \"experience\": [{\"company\": string|null, \"location\": string|null, \"title\": string|null, \"employmentType\": string|null,\n"
            + "    \"startDate\": \"YYYY-MM\"|null, \"endDate\": \"YYYY-MM\"|\"PRESENT\"|null, \"highlights\": [string], \"technologies\": [string]}],\n"
            + "  \"projects\": [{\"name\": string|null, \"description\": string|null, \"highlights\": [string], \"technologies\": [string], \"links\": [string]}],\n"
            + "  \"education\": [{\"institution\": string|null, \"location\": string|null, \"degree\": string|null, \"fieldOfStudy\": string|null,\n"
            + "    \"gpa\": string|null, \"startDate\": \"YYYY-MM\"|null, \"endDate\": \"YYYY-MM\"|null}],\n"
            + "  \"certifications\": [{\"name\": string|null, \"issuer\": string|null, \"issuedDate\": \"YYYY-MM\"|null, \"expiryDate\": \"YYYY-MM\"|null,\n"
            + "    \"credentialUrl\": string|null}],\n"
            + "  \"metadata\": {\"source\": \"LLM\", \"confidence\": number, \"warnings\": [string]}\n"
            + "}\n\n"

            + "Rules:\n"
            + "- If missing, use null for scalars and [] for arrays.\n"
            + "- Normalize bullets into concise strings.\n"
            + "- Technologies are tools/frameworks/languages explicitly mentioned.\n"
            + "- Confidence is 0..1.\n"
            + "Return only the JSON object."
        );
        messages.add(sys);

        ObjectNode user = objectMapper.createObjectNode();
        user.put("role", "user");
        user.put("content", rawText == null ? "" : rawText);
        messages.add(user);

        // (Optional) If your model supports it, uncomment:
        // body.set("response_format", objectMapper.createObjectNode().put("type", "json_object"));

        WebClient client = webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();

        
        try {
            String response = client.post()
                .uri("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            return extractAssistantMessage(response);
        } catch (WebClientResponseException e) {
            throw new RuntimeException("OpenAI error " + e.getRawStatusCode() + ": " + e.getResponseBodyAsString(), e);
        }
    }

    private String extractAssistantMessage(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode contentNode = root.path("choices").get(0).path("message").path("content");
            if (contentNode.isMissingNode()) {
                throw new RuntimeException("OpenAI response missing choices[0].message.content");
            }

            String content = contentNode.asText();

            // 1) If it is valid JSON already, return normalized JSON
            try {
                JsonNode parsed = objectMapper.readTree(content);
                return parsed.toString();
            } catch (Exception ignored) {
                // 2) Try to extract the first JSON object from the content
                String extracted = extractFirstJsonObject(content);
                JsonNode parsed = objectMapper.readTree(extracted);
                return parsed.toString();
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse OpenAI response: " + e.getMessage(), e);
        }
    }

    private String extractFirstJsonObject(String text) {
        int start = text.indexOf('{');
        if (start < 0) return text;

        int depth = 0;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '{') depth++;
            if (c == '}') {
                depth--;
                if (depth == 0) {
                    return text.substring(start, i + 1);
                }
            }
        }
        return text.substring(start);
    }

}

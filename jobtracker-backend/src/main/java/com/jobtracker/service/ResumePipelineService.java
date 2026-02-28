package com.jobtracker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobtracker.dto.resume.ResumeParseResponse;
import com.jobtracker.dto.resume.ResumeParseResultV2;
import com.jobtracker.dto.resume.ResumeStructuredData;
import com.jobtracker.exception.ResourceNotFoundException;
import com.jobtracker.model.Files;
import com.jobtracker.model.ParsedResume;
import com.jobtracker.repository.FileRepository;
import com.jobtracker.repository.ParsedResumeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ResumePipelineService {

    private static final String PARSER_VERSION = "v1";
    private static final String PARSER_VERSION_V2 = "v2";

    private final FileRepository fileRepository;
    private final ParsedResumeRepository parsedResumeRepository;
    private final S3Service s3Service;
    private final PdfTextExtractionService pdfTextExtractionService;
    private final ResumeParsingService resumeParsingService;

    private final ResumeLLMExtractionService resumeLLMExtractionService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${aws.s3.bucket:job-tracker-app-v0.1}")
    private String bucketName;

    // ==========================
    // v1 (EXISTING) - DO NOT TOUCH
    // ==========================

    public ResumeParseResponse parseResume(String fileId, String userId) {
        Files file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));

        if (!userId.equals(file.getUserId())) {
            throw new AccessDeniedException("Unauthorized access");
        }

        if (!"Resume".equals(file.getType())) {
            throw new IllegalArgumentException("File is not a Resume");
        }

        ParsedResume record = new ParsedResume();
        record.setUserId(userId);
        record.setFileId(fileId);
        record.setParserVersion(PARSER_VERSION);
        record.setParsedAt(Instant.now());

        try {
            byte[] pdfBytes = s3Service.download(bucketName, file.getS3Key());
            String rawText = pdfTextExtractionService.extractText(pdfBytes);

            // optional: store raw text (can be large; keep for now)
            record.setRawText(rawText);

            String parsedJson = resumeParsingService.parseToJson(rawText);
            record.setParsedJson(parsedJson);

            record.setStatus("SUCCESS");
            parsedResumeRepository.save(record);

            return new ResumeParseResponse(
                    fileId,
                    PARSER_VERSION,
                    "SUCCESS",
                    record.getParsedAt(),
                    parsedJson
            );

        } catch (Exception ex) {
            record.setStatus("FAILED");
            record.setErrorMessage(ex.getMessage());
            parsedResumeRepository.save(record);

            return new ResumeParseResponse(
                    fileId,
                    PARSER_VERSION,
                    "FAILED",
                    record.getParsedAt(),
                    "{}"
            );
        }
    }

    public ResumeParseResponse getLatestParsed(String fileId, String userId) {
        ParsedResume latest = parsedResumeRepository
                .findTopByUserIdAndFileIdOrderByParsedAtDesc(userId, fileId)
                .orElseThrow(() -> new ResourceNotFoundException("No parsed resume found"));

        return new ResumeParseResponse(
                fileId,
                latest.getParserVersion(),
                latest.getStatus(),
                latest.getParsedAt(),
                latest.getParsedJson() == null ? "{}" : latest.getParsedJson()
        );
    }

    // ==========================
    // v2 (NEW) - ADD NEW METHODS
    // ==========================
    
    public ResumeParseResultV2 parseResumeV2(String fileId, String userId) {
        Files file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));

        if (!userId.equals(file.getUserId())) {
            throw new AccessDeniedException("Unauthorized access");
        }

        if (!"Resume".equals(file.getType())) {
            throw new IllegalArgumentException("File is not a Resume");
        }

        ParsedResume record = new ParsedResume();
        record.setUserId(userId);
        record.setFileId(fileId);
        record.setParserVersion(PARSER_VERSION_V2);
        record.setParsedAt(Instant.now());

        try {
            byte[] pdfBytes = s3Service.download(bucketName, file.getS3Key());
            String rawText = pdfTextExtractionService.extractText(pdfBytes);

            // keep rawText stored (same field) for debugging
            record.setRawText(rawText);

            // v2 structured extraction
            ResumeStructuredData data = resumeLLMExtractionService.extract(rawText);

            if (data.getMetadata() == null) {
                ResumeStructuredData.Metadata md = new ResumeStructuredData.Metadata();
                md.setSource("LLM");
                md.setConfidence(0.7);
                data.setMetadata(md);
            } else if (data.getMetadata().getSource() == null) {
                data.getMetadata().setSource("LLM");
            }

            // store JSON string in parsedJson (same field; parserVersion distinguishes)
            String parsedJson = objectMapper.writeValueAsString(data);
            record.setParsedJson(parsedJson);

            record.setModel("gpt-5-mini");

            record.setStatus("SUCCESS");
            parsedResumeRepository.save(record);

            String preview = rawText == null ? "" : rawText.substring(0, Math.min(rawText.length(), 800));

            return new ResumeParseResultV2(
                    fileId,
                    PARSER_VERSION_V2,
                    "SUCCESS",
                    record.getParsedAt(),
                    data,
                    preview,
                    null
            );

        } catch (Exception ex) {
            record.setStatus("FAILED");
            record.setErrorMessage(ex.getMessage());
            parsedResumeRepository.save(record);

            return new ResumeParseResultV2(
                    fileId,
                    PARSER_VERSION_V2,
                    "FAILED",
                    record.getParsedAt(),
                    null,
                    "",
                    ex.getMessage()
            );
        }
    }

    public ResumeParseResultV2 getLatestParsedV2(String fileId, String userId) {
        ParsedResume latest = parsedResumeRepository
                .findTopByUserIdAndFileIdAndParserVersionOrderByParsedAtDesc(userId, fileId, PARSER_VERSION_V2)
                .orElseThrow(() -> new ResourceNotFoundException("No parsed resume found (v2)"));

        ResumeStructuredData data = null;
        String mappingError = null;

        try {
            if (latest.getParsedJson() != null && !latest.getParsedJson().isBlank()) {
                data = objectMapper.readValue(latest.getParsedJson(), ResumeStructuredData.class);
            }
        } catch (Exception e) {
            mappingError = "Failed to map stored parsedJson to ResumeStructuredData: " + e.getMessage();
        }

        String rawText = latest.getRawText();
        String preview = rawText == null ? "" : rawText.substring(0, Math.min(rawText.length(), 800));

        // If mapping failed, return FAILED even if record status was SUCCESS (client needs to know it can’t use data)
        String status = (mappingError == null) ? latest.getStatus() : "FAILED";
        String error = (mappingError == null) ? latest.getErrorMessage() : mappingError;

        return new ResumeParseResultV2(
                fileId,
                latest.getParserVersion(),
                status,
                latest.getParsedAt(),
                data,
                preview,
                error
        );
    }
}

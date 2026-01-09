package com.jobtracker.service;

import com.jobtracker.dto.ResumeParseResponse;
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

@Service
@RequiredArgsConstructor
public class ResumePipelineService {

    private static final String PARSER_VERSION = "v1";

    private final FileRepository fileRepository;
    private final ParsedResumeRepository parsedResumeRepository;
    private final S3Service s3Service;
    private final PdfTextExtractionService pdfTextExtractionService;
    private final ResumeParsingService resumeParsingService;

    @Value("${aws.s3.bucket:job-tracker-app-v0.1}")
    private String bucketName;

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
}

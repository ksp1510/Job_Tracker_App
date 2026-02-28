package com.jobtracker.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

@Service
public class FileValidationService {

    // PDF magic bytes (file signature)
    private static final byte[] PDF_MAGIC_BYTES = {0x25, 0x50, 0x44, 0x46}; // %PDF

    // Maximum file size: 10MB
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    /**
     * Validate PDF file by checking magic bytes
     * SECURITY FIX: Prevent malicious file uploads
     */
    public void validatePdfFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        // Check file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum allowed size of 10MB");
        }

        // Check content type (basic check)
        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals("application/pdf")) {
            throw new IllegalArgumentException("File must be a PDF document");
        }

        // Validate file extension
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("File extension must be .pdf");
        }

        // SECURITY FIX: Validate magic bytes (file signature)
        // This prevents users from uploading malicious files disguised as PDFs
        try (InputStream is = file.getInputStream()) {
            byte[] fileHeader = new byte[PDF_MAGIC_BYTES.length];
            int bytesRead = is.read(fileHeader);

            if (bytesRead != PDF_MAGIC_BYTES.length) {
                throw new IllegalArgumentException("Invalid PDF file: file too small");
            }

            if (!Arrays.equals(fileHeader, PDF_MAGIC_BYTES)) {
                throw new IllegalArgumentException("Invalid PDF file: file signature does not match PDF format");
            }
        }
    }

    /**
     * Sanitize filename to prevent path traversal attacks
     */
    public String sanitizeFilename(String filename) {
        if (filename == null) {
            return "unnamed";
        }

        // Remove path separators and special characters
        String sanitized = filename.replaceAll("[^a-zA-Z0-9._-]", "_");

        // Ensure filename is not empty
        if (sanitized.isEmpty() || sanitized.equals(".") || sanitized.equals("..")) {
            return "unnamed";
        }

        return sanitized;
    }
}

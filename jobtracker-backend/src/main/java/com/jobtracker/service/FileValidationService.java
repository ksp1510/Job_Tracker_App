package com.jobtracker.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

@Service
public class FileValidationService {

    private static final byte[] PDF_MAGIC_BYTES = {0x25, 0x50, 0x44, 0x46};
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    public void validatePdfFile(MultipartFile file) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File exceeds 10MB limit");
        }

        if (!"application/pdf".equalsIgnoreCase(file.getContentType())) {
            throw new IllegalArgumentException("Only PDF files are allowed");
        }

        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Invalid PDF filename");
        }

        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[PDF_MAGIC_BYTES.length];
            if (is.read(header) != PDF_MAGIC_BYTES.length ||
                !Arrays.equals(header, PDF_MAGIC_BYTES)) {
                throw new IllegalArgumentException("Invalid PDF file signature");
            }
        }
    }
}


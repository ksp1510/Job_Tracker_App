package com.jobtracker.controller;

import com.jobtracker.dto.FileDownloadResponse;
import com.jobtracker.model.Files;
import com.jobtracker.service.FileService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @PostMapping("/upload/resume")
    public ResponseEntity<Files> uploadResume(
            @RequestParam String applicationId,
            @RequestParam("file") MultipartFile file) throws IOException {

        return ResponseEntity.ok(
                fileService.uploadApplicationFile(applicationId, "Resume", file)
        );
    }

    @PostMapping("/upload/coverletter")
    public ResponseEntity<Files> uploadCoverLetter(
            @RequestParam String applicationId,
            @RequestParam("file") MultipartFile file) throws IOException {

        return ResponseEntity.ok(
                fileService.uploadApplicationFile(applicationId, "CoverLetter", file)
        );
    }

    @PostMapping(value = "/library/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Files> uploadLibrary(
            @RequestParam("type") String type,
            @RequestParam(value = "notes", required = false) String notes,
            @RequestParam("file") MultipartFile file) throws IOException {

        return ResponseEntity.ok(
                fileService.uploadLibraryFile(type, notes, file)
        );
    }

    @GetMapping
    public ResponseEntity<List<Files>> getAllFiles() {
        return ResponseEntity.ok(
                fileService.getAllFilesForCurrentUser()
        );
    }

    @GetMapping("/library")
    public ResponseEntity<List<Files>> getLibraryFiles(
            @RequestParam(required = false) String type) {

        return ResponseEntity.ok(
                fileService.getLibraryFiles(type)
        );
    }

    // ==========================
    // DOWNLOAD FILE
    // ==========================
    @GetMapping("/download/{fileId}")
    public ResponseEntity<byte[]> downloadFile(
            @PathVariable String fileId,
            Authentication authentication
    ) throws IOException {
        String userId = authentication.getName();

        FileDownloadResponse response =
                fileService.downloadFile(fileId, userId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(response.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + response.getFileName() + "\"")
                .body(response.getContent());
    }

    // ==========================
    // PRESIGNED URL
    // ==========================
    @GetMapping("/presigned-url/{fileId}")
    public ResponseEntity<Map<String, String>> getPresignedUrl(
            @PathVariable String fileId,
            Authentication authentication
    ) {
        String userId = authentication.getName();

        String url = fileService.generatePresignedUrl(fileId, userId);

        return ResponseEntity.ok(
                Map.of("url", url)
        );
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        fileService.deleteFile(id);
        return ResponseEntity.noContent().build();
    }
}

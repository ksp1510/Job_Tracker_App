package com.jobtracker.controller;

import com.jobtracker.model.Application;
import com.jobtracker.model.Files;
import com.jobtracker.model.User;
import com.jobtracker.service.S3Service;
import com.jobtracker.exception.ResourceNotFoundException;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;

import com.jobtracker.service.ApplicationService;
import com.jobtracker.config.FileNameUtil;
import com.jobtracker.repository.FileRepository;
import com.jobtracker.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.time.Duration;
import java.time.Instant;
import com.jobtracker.config.JwtUtil;

@RestController
@RequestMapping("/files")
public class FileController {

    private final S3Service s3Service;
    private final String bucketName = "job-tracker-app-v0.1";
    private final FileRepository fileRepository;
    private final ApplicationService applicationService;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final com.jobtracker.service.FileValidationService fileValidationService;

    public FileController(S3Service s3Service, FileRepository fileRepository,
                         ApplicationService applicationService, UserRepository userRepository,
                         JwtUtil jwtUtil, com.jobtracker.service.FileValidationService fileValidationService) {
        this.s3Service = s3Service;
        this.jwtUtil = jwtUtil;
        this.fileRepository = fileRepository;
        this.applicationService = applicationService;
        this.userRepository = userRepository;
        this.fileValidationService = fileValidationService;
    }

    // FIXED: Proper token extraction and user ID retrieval
    @PostMapping("/upload/resume")
    public ResponseEntity<String> uploadResume(
        @RequestParam String applicationId,
        @RequestHeader("Authorization") String authHeader,
        @RequestParam("file") MultipartFile file) throws IOException {

        // SECURITY FIX: Validate file before processing
        fileValidationService.validatePdfFile(file);

        // Extract userId from token
        String token = authHeader.replace("Bearer ", "");
        String userId = jwtUtil.getUserId(token);

        // Get application and verify ownership
        Application app = applicationService.getApplication(applicationId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        String firstName = user.getFirstName();
        String lastName = user.getLastName();
        String jobTitle = app.getJobTitle();
        String companyName = app.getCompanyName();

        String filename = FileNameUtil.generate(firstName, lastName, jobTitle, companyName, false);
        File temp = File.createTempFile("resume-", ".pdf");
        try {
            file.transferTo(temp);
            s3Service.upload(bucketName, firstName + "_" + lastName, userId, "Resumes", filename, temp);
        } finally {
            temp.delete();
        }

        Files meta = new Files();
        meta.setApplicationId(applicationId);
        meta.setUserId(userId);
        meta.setFileName(filename + ".pdf");
        meta.setType("Resume");
        meta.setFilePath("s3://" + bucketName + "/" + firstName + "_" + lastName + "_" + userId + "/Resumes/" + filename + ".pdf");
        meta.setUploadedAt(Instant.now());
        meta.setNotes("Uploaded by " + firstName + " " + lastName);
        fileRepository.save(meta);

        app.setResumeId(meta.getId());
        applicationService.updateApplication(applicationId, userId, app);

        return ResponseEntity.ok("Uploaded Resume: " + filename + ".pdf");
    }

    // FIXED: Proper token extraction and user ID retrieval
    @PostMapping("/upload/coverletter")
    public ResponseEntity<String> uploadCoverLetter(
            @RequestParam String applicationId,
            @RequestHeader("Authorization") String authHeader,
            @RequestParam("file") MultipartFile file) throws IOException {

        // SECURITY FIX: Validate file before processing
        fileValidationService.validatePdfFile(file);

        // Extract userId from token
        String token = authHeader.replace("Bearer ", "");
        String userId = jwtUtil.getUserId(token);

        Application app = applicationService.getApplication(applicationId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        String firstName = user.getFirstName();
        String lastName = user.getLastName();
        String jobTitle = app.getJobTitle();
        String companyName = app.getCompanyName();

        String filename = FileNameUtil.generate(firstName, lastName, jobTitle, companyName, false);
        File temp = File.createTempFile("coverletter-", ".pdf");
        try {
            file.transferTo(temp);
            s3Service.upload(bucketName, firstName + "_" + lastName, userId, "CoverLetters", filename, temp);
        } finally {
            temp.delete();
        }

        Files meta = new Files();
        meta.setApplicationId(applicationId);
        meta.setUserId(userId);
        meta.setFileName(filename + ".pdf");
        meta.setType("CoverLetter");
        meta.setFilePath("s3://" + bucketName + "/" + firstName + "_" + lastName + "_" + userId + "/CoverLetters/" + filename + ".pdf");
        meta.setUploadedAt(Instant.now());
        meta.setNotes("Uploaded by " + firstName + " " + lastName);
        fileRepository.save(meta);

        app.setCoverLetterId(meta.getId());
        applicationService.updateApplication(applicationId, userId, app);

        return ResponseEntity.ok("Uploaded Cover Letter: " + filename + ".pdf");
    }

    @GetMapping
    public ResponseEntity<List<Files>> listFiles(
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String userId = jwtUtil.getUserId(token);
        return ResponseEntity.ok(fileRepository.findByUserId(userId));
    }

    @GetMapping("/applications/{applicationId}/files")
    public ResponseEntity<List<Files>> getFilesByApplication(
            @PathVariable String applicationId,
            @RequestHeader("Authorization") String authHeader) {
        // SECURITY FIX: Verify ownership before returning files
        String token = authHeader.replace("Bearer ", "");
        String userId = jwtUtil.getUserId(token);

        Application app = applicationService.getApplication(applicationId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        List<Files> files = fileRepository.findByApplicationId(applicationId);
        if (files.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(files);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteFile(
            @PathVariable String id,
            @RequestHeader("Authorization") String authHeader) {
        // SECURITY FIX: Verify ownership before deletion
        String token = authHeader.replace("Bearer ", "");
        String userId = jwtUtil.getUserId(token);

        Files fileMeta = fileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));

        // Verify the file belongs to the user
        if (!fileMeta.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("File not found");
        }

        // Extract key directly from filePath by removing s3://bucketName/
        String key = fileMeta.getFilePath()
                .replace("s3://" + bucketName + "/", "");

        s3Service.deleteFile(bucketName, key);
        fileRepository.deleteById(id);
        applicationService.fileDeleted(fileMeta.getApplicationId(), fileMeta.getType());

        return ResponseEntity.ok("Deleted: " + fileMeta.getFileName());
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<byte[]> downloadFile(
            @PathVariable String id,
            @RequestHeader("Authorization") String authHeader) {
        // SECURITY FIX: Verify ownership before download
        String token = authHeader.replace("Bearer ", "");
        String userId = jwtUtil.getUserId(token);

        Files fileMeta = fileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));

        // Verify the file belongs to the user
        if (!fileMeta.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("File not found");
        }

        // Extract S3 key from filePath
        String key = fileMeta.getFilePath().replace("s3://" + bucketName + "/", "");

        byte[] content = null;
        try {
            content = s3Service.download(bucketName, key);
        } catch (AwsServiceException | SdkClientException | IOException e) {
            throw new RuntimeException("Failed to download file", e);
        }

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + fileMeta.getFileName() + "\"")
                .body(content);
    }

    @GetMapping("/presigned/{id}")
    public ResponseEntity<String> getPresignedUrl(
            @PathVariable String id,
            @RequestHeader("Authorization") String authHeader) {
        // SECURITY FIX: Verify ownership before generating presigned URL
        String token = authHeader.replace("Bearer ", "");
        String userId = jwtUtil.getUserId(token);

        Files fileMeta = fileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));

        // Verify the file belongs to the user
        if (!fileMeta.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("File not found");
        }

        String key = fileMeta.getFilePath().replace("s3://" + bucketName + "/", "");

        // Generate a presigned URL valid for 1 hour
        String url = s3Service.generatePresignedUrl(bucketName, key, Duration.ofHours(1)).toString();

        return ResponseEntity.ok(url);
    }
}
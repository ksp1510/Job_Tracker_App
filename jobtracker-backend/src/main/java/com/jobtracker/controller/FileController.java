package com.jobtracker.controller;

import com.jobtracker.model.Application;
import com.jobtracker.model.Files;
import com.jobtracker.service.S3Service;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;

import com.jobtracker.service.ApplicationService;
import com.jobtracker.config.FileNameUtil;
import com.jobtracker.repository.FileRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.time.Duration;
import java.time.Instant;

@RestController
@RequestMapping("/files")
public class FileController {

    private final S3Service s3Service;
    private final String bucketName = "job-tracker-app-v0.1";
    private final FileRepository fileRepository;
    private final ApplicationService applicationService;
    
    public FileController(S3Service s3Service, FileRepository fileRepository, ApplicationService applicationService) {
        this.s3Service = s3Service;
        this.fileRepository = fileRepository;
        this.applicationService = applicationService;
    }

    @PostMapping("/upload/resume")
public ResponseEntity<String> uploadResume(
        @RequestParam String jobTitle,
        @RequestParam String firstName,
        @RequestParam String lastName,
        @RequestParam String userId,
        @RequestParam String applicationId,
        @RequestParam(required = false) String company,
        @RequestParam("file") MultipartFile file) throws IOException {

    String filename = FileNameUtil.generate(firstName, lastName, jobTitle, company, false);
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

    Application app = new Application();
    app.setResumeId(meta.getId());
    applicationService.update(applicationId, app);

    return ResponseEntity.ok("Uploaded Resume: " + filename + ".pdf");
}


    @PostMapping("/upload/coverletter")
    public ResponseEntity<String> uploadCoverLetter(
            @RequestParam String jobTitle,
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam String userId,
            @RequestParam String applicationId,
            @RequestParam(required = false) String company,
            @RequestParam("file") MultipartFile file) throws IOException {

        String filename = FileNameUtil.generate(firstName, lastName, jobTitle, company, false);
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

        Application app = new Application();
        app.setCoverLetterId(meta.getId());
        applicationService.update(applicationId, app);

        return ResponseEntity.ok("Uploaded Cover Letter: " + filename + ".pdf");
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<Files>> listFiles(
            @PathVariable String userId) {
        return ResponseEntity.ok(fileRepository.findByUserId(userId));
    }

    @GetMapping("/applications/{applicationId}/files")
    public ResponseEntity<List<Files>> getFilesByApplication(@PathVariable String applicationId) {
        List<Files> files = fileRepository.findByApplicationId(applicationId);
        if (files.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(files);
}


    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteFile(@PathVariable String id) {
        Files fileMeta = fileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("File not found"));

        // Extract key directly from filePath by removing s3://bucketName/
        String key = fileMeta.getFilePath()
                .replace("s3://" + bucketName + "/", "");

        s3Service.deleteFile(bucketName, key);
        fileRepository.deleteById(id);
        applicationService.fileDeleted(fileMeta.getApplicationId(), fileMeta.getType());

        return ResponseEntity.ok("Deleted: " + fileMeta.getFileName());
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<byte[]> downloadFile(@PathVariable String id) {
        Files fileMeta = fileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("File not found"));

        // Extract S3 key from filePath
        String key = fileMeta.getFilePath().replace("s3://" + bucketName + "/", "");

        byte[] content = null;
        try {
            content = s3Service.download(bucketName, key);
        } catch (AwsServiceException | SdkClientException | IOException e) {
            e.printStackTrace();
        }

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + fileMeta.getFileName() + ".pdf\"")
                .body(content);
    }

    @GetMapping("/presigned/{id}")
    public ResponseEntity<String> getPresignedUrl(@PathVariable String id) {
        Files fileMeta = fileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("File not found"));

        String key = fileMeta.getFilePath().replace("s3://" + bucketName + "/", "");

        // Generate a presigned URL valid for 1 hour
        String url = s3Service.generatePresignedUrl(bucketName, key, Duration.ofHours(1)).toString();

        return ResponseEntity.ok(url);
    }


}

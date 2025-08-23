package com.jobtracker.controller;

import com.jobtracker.model.Application;
import com.jobtracker.model.Files;
import com.jobtracker.service.S3Service;
import com.jobtracker.service.ApplicationService;
import com.jobtracker.config.FileNameUtil;
import com.jobtracker.repository.FileRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
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

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteFile(@PathVariable String id) {
        Files fileMeta = fileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("File not found"));

        // Extract key directly from filePath by removing s3://bucketName/
        String key = fileMeta.getFilePath()
                .replace("s3://" + bucketName + "/", "");

        s3Service.deleteFile(bucketName, key);
        fileRepository.deleteById(id);

        return ResponseEntity.ok("Deleted: " + fileMeta.getFileName());
    }

}

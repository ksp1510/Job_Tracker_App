package com.jobtracker.controller;

import com.jobtracker.service.S3Service;
import com.jobtracker.config.FileNameUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/files")
public class FileController {

    private final S3Service s3Service;
    private final String bucketName = "your-bucket-name";

    public FileController(S3Service s3Service) {
        this.s3Service = s3Service;
    }

    @PostMapping("/upload/resume")
    public ResponseEntity<String> uploadResume(
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam String jobTitle,
            @RequestParam String userName,
            @RequestParam String userId,
            @RequestParam(required = false) String company,
            @RequestParam("file") MultipartFile file) throws IOException {

        String filename = FileNameUtil.generate(firstName, lastName, jobTitle, company, false);
        File temp = File.createTempFile("resume-", ".pdf");
        file.transferTo(temp);

        s3Service.upload(bucketName, userName, userId, "Resumes", filename, temp);
        temp.delete();

        return ResponseEntity.ok("Uploaded Resume: " + filename + ".pdf");
    }

    @PostMapping("/upload/coverletter")
    public ResponseEntity<String> uploadCoverLetter(
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam String jobTitle,
            @RequestParam String userName,
            @RequestParam String userId,
            @RequestParam(required = false) String company,
            @RequestParam("file") MultipartFile file) throws IOException {

        String filename = FileNameUtil.generate(firstName, lastName, jobTitle, company, false);
        File temp = File.createTempFile("coverletter-", ".pdf");
        file.transferTo(temp);

        s3Service.upload(bucketName, userName, userId, "CoverLetters", filename, temp);
        temp.delete();

        return ResponseEntity.ok("Uploaded Cover Letter: " + filename + ".pdf");
    }

    @GetMapping("/{userName}/{userId}/{type}")
    public ResponseEntity<List<String>> listFiles(
            @PathVariable String userName,
            @PathVariable String userId,
            @PathVariable String type) {
        return ResponseEntity.ok(s3Service.listFiles(bucketName, userName, userId, type));
    }

    @DeleteMapping("/delete/{userName}/{userId}/{type}/{fileName}")
    public ResponseEntity<String> deleteFile(
            @PathVariable String userName,
            @PathVariable String userId,
            @PathVariable String type,
            @PathVariable String fileName) {
        s3Service.deleteFile(bucketName, userName, userId, type, fileName);
        return ResponseEntity.ok("Deleted: " + fileName);
    }
}

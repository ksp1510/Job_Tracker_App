package com.jobtracker.service;

import com.jobtracker.config.FileNameUtil;
import com.jobtracker.dto.FileDownloadResponse;
import com.jobtracker.exception.ResourceNotFoundException;
import com.jobtracker.model.Application;
import com.jobtracker.model.Files;
import com.jobtracker.model.User;
import com.jobtracker.repository.FileRepository;
import com.jobtracker.repository.UserRepository;
import com.jobtracker.util.UserContext;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.core.ResponseInputStream;

import java.time.Duration;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FileService {

    private final S3Service s3Service;
    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final ApplicationService applicationService;
    private final FileValidationService fileValidationService;


    @Value("${aws.s3.bucket:job-tracker-app-v0.1}")
    private String bucketName;

    public Files uploadLibraryFile(String type, String notes, MultipartFile file) throws IOException {
        validateType(type);
        fileValidationService.validatePdfFile(file);

        String userId = UserContext.getUserId();
        User user = getUser(userId);

        String folder = folderForType(type);

        int nextVersion = fileRepository
                .findTopByUserIdAndTypeAndReusableTrueOrderByVersionDesc(userId, type)
                .map(f -> (f.getVersion() == null ? 0 : f.getVersion()) + 1)
                .orElse(1);

        String filenameBase = FileNameUtil.generate(
                user.getFirstName(),
                user.getLastName(),
                folder + "_v" + nextVersion,
                null,
                true
        );

        String s3Key = buildS3Key(user, folder, filenameBase);

        uploadToS3(file, s3Key);

        Files meta = new Files();
        meta.setUserId(userId);
        meta.setApplicationId(null);
        meta.setReusable(true);
        meta.setType(type);
        meta.setVersion(nextVersion);
        meta.setFileName(filenameBase + ".pdf");
        meta.setS3Key(s3Key);
        meta.setUploadedAt(Instant.now());
        meta.setNotes((notes == null || notes.isBlank()) ? ("Library upload v" + nextVersion) : notes);

        return fileRepository.save(meta);
    }

    public Files uploadApplicationFile(String applicationId, String type, MultipartFile file) throws IOException {
        validateType(type);
        fileValidationService.validatePdfFile(file);

        String userId = UserContext.getUserId();

        Application app = applicationService.getApplication(applicationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        User user = getUser(userId);

        String folder = folderForType(type);

        String filenameBase = FileNameUtil.generate(
                user.getFirstName(),
                user.getLastName(),
                app.getJobTitle(),
                app.getCompanyName(),
                false
        );

        String s3Key = buildS3Key(user, folder, filenameBase);

        uploadToS3(file, s3Key);

        Files meta = new Files();
        meta.setUserId(userId);
        meta.setApplicationId(applicationId);
        meta.setReusable(false);
        meta.setType(type);
        meta.setFileName(filenameBase + ".pdf");
        meta.setS3Key(s3Key);
        meta.setUploadedAt(Instant.now());
        meta.setNotes("Uploaded for application");

        Files saved = fileRepository.save(meta);

        if ("Resume".equals(type)) {
            app.setResumeId(saved.getId());
        } else {
            app.setCoverLetterId(saved.getId());
        }

        applicationService.updateApplication(applicationId, userId, app);
        return saved;
    }
                                              
    public List<Files> getAllFilesForCurrentUser() {
        String userId = UserContext.getUserId();
        return fileRepository.findByUserId(userId);
    }

    public List<Files> getLibraryFiles(String type) {
        String userId = UserContext.getUserId();

        if (type == null || type.isBlank()) {
            return fileRepository
                    .findByUserIdAndReusableTrueOrderByUploadedAtDesc(userId);
        }

        validateType(type);
        return fileRepository
                .findByUserIdAndTypeAndReusableTrueOrderByUploadedAtDesc(userId, type);
    }

    // ==========================
    // DOWNLOAD FILE
    // ==========================
    public FileDownloadResponse downloadFile(String fileId, String userId) throws IOException {

        Files file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));

        if (!file.getUserId().equals(userId)) {
            throw new AccessDeniedException("Unauthorized access");
        }

        byte[] content = s3Service.download(bucketName, file.getS3Key());

        return new FileDownloadResponse(
                content,
                file.getFileName(),
                "application/pdf"
        );
    }


    // ==========================
    // PRESIGNED URL
    // ==========================
    public String generatePresignedUrl(String fileId, String userId) {

        Files file = fileRepository.findById(fileId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("File not found"));

        if (!file.getUserId().equals(userId)) {
            throw new AccessDeniedException("Unauthorized access");
        }

        GetObjectRequest getObjectRequest =
                GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(file.getS3Key())
                        .build();

        GetObjectPresignRequest presignRequest =
                GetObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofMinutes(10))
                        .getObjectRequest(getObjectRequest)
                        .build();

        return s3Service.generatePresignedUrl(bucketName, file.getS3Key(), Duration.ofMinutes(10)).toString();
    }



    public void deleteFile(String fileId) {
        String userId = UserContext.getUserId();

        Files file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));

        if (!userId.equals(file.getUserId())) {
            throw new ResourceNotFoundException("File not found");
        }

        if (file.getS3Key() == null || file.getS3Key().isBlank()) {
            throw new IllegalStateException("File is missing s3Key");
        }

        s3Service.deleteFile(bucketName, file.getS3Key());
        fileRepository.deleteById(fileId);

        // Only update application if it was tied to an application
        if (file.getApplicationId() != null && !file.getApplicationId().isBlank()) {
            applicationService.fileDeleted(file.getApplicationId(), file.getType());
        }
    }

    /* =========================
       Helpers
       ========================= */

    private void uploadToS3(MultipartFile multipart, String s3Key) throws IOException {
        File temp = File.createTempFile("upload-", ".pdf");
        try {
            multipart.transferTo(temp);
            s3Service.upload(bucketName, s3Key, temp);
        } finally {
            //noinspection ResultOfMethodCallIgnored
            temp.delete();
        }
    }

    private User getUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private String folderForType(String type) {
        // Keep folder names stable in S3
        return "Resume".equals(type) ? "Resumes" : "CoverLetters";
    }

    private String buildS3Key(User user, String folder, String filenameBase) {
        return user.getFirstName() + "_" +
            user.getLastName() + "_" +
            user.getUserId() + "/" +
            folder + "/" +
            filenameBase + ".pdf";
    }

    private void validateType(String type) {
        if (!"Resume".equals(type) && !"CoverLetter".equals(type)) {
            throw new IllegalArgumentException("Type must be Resume or CoverLetter");
        }
    }
}

package com.jobtracker.service;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

import org.springframework.stereotype.Service;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class S3Service {
    private final S3Client s3 = S3Client.builder()
        .region(Region.US_EAST_1) // match your config
        .credentialsProvider(DefaultCredentialsProvider.create())
        .build();

    public void upload(String bucket, String username, String userId, 
                       String type, String filename, File file) {
        String key = username + userId + "/" + type + "/" + filename + ".pdf";
        PutObjectRequest putReq = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
        s3.putObject(putReq, Path.of(file.getAbsolutePath()));
    }

    public List<String> listFiles(String bucketName, String userName, String userId, String type) {
        String prefix = userName + userId + "/" + type + "/";

        ListObjectsV2Response result = s3.listObjectsV2(ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix(prefix)
                .build());

        return result.contents().stream()
                .map(S3Object::key)
                .collect(Collectors.toList());
    }

    public void deleteFile(String bucketName, String userName, String userId, String type, String fileName) {
        String key = userName + userId + "/" + type + "/" + fileName;

        DeleteObjectRequest delReq = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        s3.deleteObject(delReq);
    }
}

package com.jobtracker.service;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.InvalidObjectStateException;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import org.springframework.stereotype.Service;

import com.jobtracker.model.User;
import com.jobtracker.repository.UserRepository;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;


@Service
public class S3Service {
    private final UserRepository userRepository;
    private final S3Presigner presigner;

    public S3Service(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.presigner = S3Presigner.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
    private final S3Client s3 = S3Client.builder()
        .region(Region.US_EAST_1) // match your config
        .credentialsProvider(DefaultCredentialsProvider.create())
        .build();

    public void upload(String bucket, String username, String userId, 
                       String type, String filename, File file) {
        String key = username + "_" + userId + "/" + type + "/" + filename + ".pdf";
        PutObjectRequest putReq = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType("application/pdf")
                .build();
        s3.putObject(putReq, Path.of(file.getAbsolutePath()));
    }

    public List<String> listFiles(String bucketName, String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        String prefix = user.getFirstName() + "_" + user.getLastName() + "_" + userId + "/";

        ListObjectsV2Response result = s3.listObjectsV2(ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix(prefix)
                .build());

        return result.contents().stream()
                .map(S3Object::key)
                .collect(Collectors.toList());
    }

    public void deleteFile(String bucketName, String key) {
        DeleteObjectRequest delReq = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        s3.deleteObject(delReq);
    }


    /** ✅ Download file 
     * @throws IOException 
     * @throws SdkClientException 
     * @throws AwsServiceException 
     * @throws S3Exception 
     * @throws InvalidObjectStateException 
     * @throws NoSuchKeyException */
    public byte[] download(String bucket, String key) throws NoSuchKeyException, InvalidObjectStateException, S3Exception, AwsServiceException, SdkClientException, IOException {
        return s3.getObject(GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build()).readAllBytes();
    }

    /** ✅ Generate presigned URL */
    public URL generatePresignedUrl(String bucket, String key, Duration expiry) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(expiry)
                .getObjectRequest(getObjectRequest)
                .build();

        return presigner.presignGetObject(presignRequest).url();
    }
}

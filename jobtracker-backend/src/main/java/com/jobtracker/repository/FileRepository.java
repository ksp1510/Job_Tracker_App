package com.jobtracker.repository;

import com.jobtracker.model.Files;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface FileRepository extends MongoRepository<Files, String> {

    List<Files> findByUserId(String userId);

    Optional<Files> findById(String fileId);

    List<Files> findByApplicationId(String applicationId);

    // ===== NEW METHODS =====

    List<Files> findByUserIdAndReusableTrueOrderByUploadedAtDesc(String userId);

    List<Files> findByUserIdAndTypeAndReusableTrueOrderByUploadedAtDesc(String userId, String type);

    Optional<Files> findTopByUserIdAndTypeAndReusableTrueOrderByVersionDesc(String userId, String type);
}

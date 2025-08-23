package com.jobtracker.repository;

import com.jobtracker.model.Files;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface FileRepository extends MongoRepository<Files, String> {
    List<Files> findByApplicationIdAndType(String applicationId, String type);

    Optional<Files> findById(String id);

    void deleteByApplicationIdAndType(String applicationId, String type);

    List<Files> findByUserId(String userId);


}


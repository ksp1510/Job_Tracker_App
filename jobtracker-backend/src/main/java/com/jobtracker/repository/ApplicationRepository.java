package com.jobtracker.repository;

import com.jobtracker.model.Application;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends MongoRepository<Application, String> {
    Optional<Application> findById(String id);
    List<Application> findByUserId(String userId);
    List<Application> findByStatus(String status);
    List<Application> findByUserIdAndStatus(String userId, String status);
    Optional<Application> findByIdAndUserId(String id, String userId);
}

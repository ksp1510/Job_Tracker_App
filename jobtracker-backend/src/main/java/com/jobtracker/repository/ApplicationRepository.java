package com.jobtracker.repository;

import com.jobtracker.model.Application;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface ApplicationRepository extends MongoRepository<Application, String> {
    List<Application> findByUserId(String userId);
}

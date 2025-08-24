package com.jobtracker.repository;

import com.jobtracker.model.Application;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.time.LocalDate;

public interface ApplicationRepository extends MongoRepository<Application, String> {
    List<Application> findByUserId(String userId);

    List<Application> findByStatus(String status);

    List<Application> findByStatusAndLastFollowUpDateBefore(String status, LocalDate threshold);
}

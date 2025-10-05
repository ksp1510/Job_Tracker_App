package com.jobtracker.repository;

import com.jobtracker.model.Application;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends MongoRepository<Application, String> {
    List<Application> findByUserId(String userId);
    
    List<Application> findByUserIdOrderByAppliedDateDesc(String userId);
    
    Optional<Application> findByIdAndUserId(String id, String userId);
    
    List<Application> findByUserIdAndStatus(String userId, String status);

    // Find by external job ID to check if already applied
    Optional<Application> findByUserIdAndExternalJobId(String userId, String externalJobId);
    
    // Find applications needing follow-up (status hasn't changed in 7 days)
    @Query("{ 'appliedDate': { $lte: ?0 }, 'status': 'APPLIED', " +
           "$or: [ { 'lastStatusChangeDate': null }, { 'lastStatusChangeDate': { $lte: ?0 } } ] }")
    List<Application> findApplicationsNeedingFollowUp(LocalDateTime sevenDaysAgo);
    
    // Count by status
    long countByUserIdAndStatus(String userId, String status);
}

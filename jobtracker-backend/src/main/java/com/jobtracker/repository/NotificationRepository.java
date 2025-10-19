package com.jobtracker.repository;

import com.jobtracker.model.Notification;
import com.jobtracker.model.Notification.NotificationType;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends MongoRepository<Notification, String> {
    // FIXED: Added missing method
    List<Notification> findByUserId(String userId);

    
    
    List<Notification> findByUserIdOrderByNotifyAtDesc(String userId);
    
    List<Notification> findByUserIdAndReadFalseOrderByNotifyAtDesc(String userId);
    
    List<Notification> findByUserIdAndReadFalse(String userId);
    
    // Find notifications that should be sent now
    List<Notification> findBySentFalseAndNotifyAtBefore(LocalDateTime dateTime);
    
    // Check if follow-up reminder already exists
    boolean existsByApplicationIdAndTypeAndCreatedAtAfter(
        String applicationId, 
        NotificationType type, 
        LocalDateTime createdAfter
    );
}
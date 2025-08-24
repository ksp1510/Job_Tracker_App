package com.jobtracker.repository;

import com.jobtracker.model.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends MongoRepository<Notification, String> {
    List<Notification> findByUserId(String userId);
    List<Notification> findByUserIdAndReadFalse(String userId);
    List<Notification> findBySentFalseAndNotifyAtBefore(LocalDateTime now);
}

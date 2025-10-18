package com.jobtracker.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.jobtracker.model.NotificationPreference;

import org.springframework.stereotype.Repository;

@Repository
public interface NotificationPreferenceRepository extends MongoRepository<NotificationPreference, String> {
    
    Optional<NotificationPreference> findByUserId(String userId);

    NotificationPreference save(NotificationPreference pref);
}

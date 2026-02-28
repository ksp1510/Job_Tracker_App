package com.jobtracker.repository;

import com.jobtracker.model.CandidateProfile;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface CandidateProfileRepository extends MongoRepository<CandidateProfile, String> {
    Optional<CandidateProfile> findTopByUserIdOrderByGeneratedAtDesc(String userId);
}

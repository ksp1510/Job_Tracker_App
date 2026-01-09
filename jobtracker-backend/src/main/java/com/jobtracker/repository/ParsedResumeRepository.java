package com.jobtracker.repository;

import com.jobtracker.model.ParsedResume;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ParsedResumeRepository extends MongoRepository<ParsedResume, String> {

    Optional<ParsedResume> findTopByUserIdAndFileIdAndParserVersionOrderByParsedAtDesc(
            String userId,
            String fileId,
            String parserVersion
    );

    Optional<ParsedResume> findTopByUserIdAndFileIdOrderByParsedAtDesc(String userId, String fileId);
}

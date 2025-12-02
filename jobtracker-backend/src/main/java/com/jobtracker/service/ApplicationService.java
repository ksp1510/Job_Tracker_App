package com.jobtracker.service;

import com.jobtracker.model.Application;
import lombok.RequiredArgsConstructor;
import com.jobtracker.repository.ApplicationRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;

    public Application createApplication(Application application) {
        application.setCreatedAt(Instant.now());
        application.setUpdatedAt(Instant.now());
        application.setLastStatusChangeDate(Instant.now());
        return applicationRepository.save(application);
    }

    public Application updateApplication(String id, String userId, Application updatedApplication) {
        Application existing = applicationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        // Track status changes
        if (!existing.getStatus().equals(updatedApplication.getStatus())) {
            updatedApplication.setLastStatusChangeDate(Instant.now());
        }

        updatedApplication.setId(existing.getId());
        updatedApplication.setUserId(userId);
        updatedApplication.setCreatedAt(existing.getCreatedAt());
        updatedApplication.setUpdatedAt(Instant.now());
        
        return applicationRepository.save(updatedApplication);
    }

    public List<Application> getUserApplications(String userId) {
        return applicationRepository.findByUserIdOrderByAppliedDateDesc(userId);
    }

    public Optional<Application> getApplication(String id, String userId) {
        return applicationRepository.findByIdAndUserId(id, userId);
    }

    public void deleteApplication(String id, String userId) {
        Application application = applicationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Application not found"));
        applicationRepository.delete(application);
    }

    public List<Application> findByUserIdAndStatus(String userId, String status) {
        return applicationRepository.findByUserIdAndStatus(userId, status);
    }

    // Check if user has already applied to this job
    public boolean hasAppliedToJob(String userId, String externalJobId) {
        return applicationRepository.findByUserIdAndExternalJobId(userId, externalJobId).isPresent();
    }

    // FIXED: Proper implementation of fileDeleted
    public void fileDeleted(String applicationId, String type) {
        Optional<Application> appOpt = applicationRepository.findById(applicationId);
        if (appOpt.isEmpty()) {
            return;
        }
        
        Application app = appOpt.get();
        if ("Resume".equals(type)) {
            app.setResumeId(null);
        } else if ("CoverLetter".equals(type)) {
            app.setCoverLetterId(null);
        }
        
        app.setUpdatedAt(Instant.now());
        applicationRepository.save(app);
    }
}
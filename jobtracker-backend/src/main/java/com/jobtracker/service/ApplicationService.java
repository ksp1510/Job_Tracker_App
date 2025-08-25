package com.jobtracker.service;

import com.jobtracker.model.Application;
import com.jobtracker.model.Status;
import com.jobtracker.repository.ApplicationRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.time.LocalDate;

@Service
public class ApplicationService {

    private final ApplicationRepository repository;
    private final NotificationService notificationService;

    public ApplicationService(ApplicationRepository repository, NotificationService notificationService) {
        this.repository = repository;
        this.notificationService = notificationService;
    }

    public Application save(String userId, Application app) {
        app.setUserId(userId);
        app.setAppliedDate(LocalDate.now());

        //Save the application first
        Application savedApp = repository.save(app);

        //Create follwoe-up reminder if status is APPLIED
        if (app.getStatus() == Status.APPLIED) {
            try {
                notificationService.createFollowUpReminder(savedApp);
            } catch (Exception e) {
                System.err.println("⚠️ Failed to create follow-up reminder: " + e.getMessage());
                // Don't fail the application creation if notification creation fails
            }
        }
        return savedApp;
    }

    public List<Application> getAllByUser(String userId) {
        return repository.findByUserId(userId);
    }

    public Application getById(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found: " + id));
    }

    public Application findByIdAndUserId(String id, String userId) {
        return repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Application not found: " + id));
    }

    public List<Application> findByStatus(String status) {
        return repository.findByStatus(status);
    }

    public List<Application> findByUserIdAndStatus(String userId, String status) {
        return repository.findByUserIdAndStatus(userId, status);
    }

    public Application update(String id, Application appDetails) {
        return repository.findById(id).map(app -> {
            if (appDetails.getCompanyName() != null) app.setCompanyName(appDetails.getCompanyName());
            if (appDetails.getJobTitle() != null) app.setJobTitle(appDetails.getJobTitle());
            if (appDetails.getJobLocation() != null) app.setJobLocation(appDetails.getJobLocation());
            if (appDetails.getJobDescription() != null) app.setJobDescription(appDetails.getJobDescription());
            if (appDetails.getJobLink() != null) app.setJobLink(appDetails.getJobLink());
            if (appDetails.getRecruiterContact() != null) app.setRecruiterContact(appDetails.getRecruiterContact());
            if (appDetails.getStatus() != null) app.setStatus(appDetails.getStatus());
            if (appDetails.getSalary() != null) app.setSalary(appDetails.getSalary());
            if (appDetails.getNotes() != null) app.setNotes(appDetails.getNotes());
            if (appDetails.getResumeId() != null) app.setResumeId(appDetails.getResumeId());
            if (appDetails.getCoverLetterId() != null) app.setCoverLetterId(appDetails.getCoverLetterId());
            if (appDetails.getAppliedDate() != null) app.setAppliedDate(appDetails.getAppliedDate());
            if (appDetails.getLastFollowUpDate() != null) app.setLastFollowUpDate(appDetails.getLastFollowUpDate());
            if (appDetails.getReferral() != null) app.setReferral(appDetails.getReferral());
            if (appDetails.getInterviewDate() != null) app.setInterviewDate(appDetails.getInterviewDate());
            if (appDetails.getAssessmentDate() != null) app.setAssessmentDate(appDetails.getAssessmentDate());
            return repository.save(app);
        }).orElseThrow(() -> new RuntimeException("Application not found: " + id));
    }

    public void delete(String id) {
        repository.deleteById(id);
    }

    public void fileDeleted(String applicationId, String fileType) {
        repository.findById(applicationId).map(app -> {
            if ("Resume".equalsIgnoreCase(fileType)) {
                app.setResumeId(null);
            } else if ("CoverLetter".equalsIgnoreCase(fileType)) {
                app.setCoverLetterId(null);
            }
            return repository.save(app);
        }).orElseThrow(() -> new RuntimeException("Application not found: " + applicationId));
    }
    
}

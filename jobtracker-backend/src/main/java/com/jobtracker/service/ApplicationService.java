package com.jobtracker.service;

import com.jobtracker.model.Application;
import com.jobtracker.model.Status;
import com.jobtracker.repository.ApplicationRepository;
import com.jobtracker.service.NotificationService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.time.LocalDate;
import java.util.Optional;

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
        Application saved = repository.save(app);
    
        // Auto-create reminder 7 days later
        notificationService.createFollowUpReminder(saved, 7);

         // Auto-create reminders
        if (saved.getStatus() == Status.INTERVIEW) {
            notificationService.createInterviewReminder(saved);
        } else {
            notificationService.createFollowUpReminder(saved, 7);
        }

        return saved;
    }
    

    public List<Application> getAllByUser(String userId) {
        return repository.findByUserId(userId);
    }

    public Optional<Application> getById(String id) {
        return repository.findById(id);
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

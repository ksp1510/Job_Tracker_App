package com.jobtracker.service;

import com.jobtracker.model.Application;
import com.jobtracker.repository.ApplicationRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.time.LocalDate;
import java.util.Optional;

@Service
public class ApplicationService {

    private final ApplicationRepository repository;

    public ApplicationService(ApplicationRepository repository) {
        this.repository = repository;
    }

    public Application save(String userId, Application app) {
        app.setUserId(userId);
        app.setAppliedDate(LocalDate.now());
        return repository.save(app);
    }

    public List<Application> getAllByUser(String userId) {
        return repository.findByUserId(userId);
    }

    public Optional<Application> getById(String id) {
        return repository.findById(id);
    }

    public Application update(String id, Application appDetails) {
        return repository.findById(id).map(app -> {
            app.setCompanyName(appDetails.getCompanyName());
            app.setJobTitle(appDetails.getJobTitle());
            app.setJobDescription(appDetails.getJobDescription());
            app.setJobLink(appDetails.getJobLink());
            app.setRecruiterContact(appDetails.getRecruiterContact());
            app.setStatus(appDetails.getStatus());
            app.setSalary(appDetails.getSalary());
            app.setNotes(appDetails.getNotes());
            app.setResumeId(appDetails.getResumeId());
            app.setCoverLetterId(appDetails.getCoverLetterId());
            app.setAppliedDate(appDetails.getAppliedDate());
            app.setLastFollowUpDate(appDetails.getLastFollowUpDate());
            app.setReferral(appDetails.getReferral());
            return repository.save(app);
        }).orElseThrow(() -> new RuntimeException("Application not found: " + id));
    }

    public void delete(String id) {
        repository.deleteById(id);
    }
}

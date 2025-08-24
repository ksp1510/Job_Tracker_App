package com.jobtracker.service;

import com.jobtracker.model.Application;
import com.jobtracker.model.Notification;
import com.jobtracker.repository.NotificationRepository;
import com.jobtracker.service.EmailService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService; // for email

    public NotificationService(NotificationRepository notificationRepository, EmailService emailService) {
        this.notificationRepository = notificationRepository;
        this.emailService = emailService;
    }

    public void createFollowUpReminder(Application app, int daysUntilFollowUp) {
        Notification n = new Notification();
        n.setUserId(app.getUserId());
        n.setApplicationId(app.getId());
        n.setMessage("Follow up on " + app.getCompanyName() + " - " + app.getJobTitle());
        n.setNotifyAt(LocalDateTime.now().plusDays(daysUntilFollowUp));
        n.setSent(false);

        notificationRepository.save(n);
    }

    public Notification createNotification(Notification notification) {
        return notificationRepository.save(notification);
    }

    public List<Notification> getUserNotifications(String userId) {
        return notificationRepository.findByUserId(userId);
    }

    public void processDueNotifications() {
        List<Notification> due = notificationRepository.findBySentFalseAndNotifyAtBefore(LocalDateTime.now());

        for (Notification n : due) {
            // 1. Send via SES (email)
            emailService.sendEmail(
                    "ksp1510@gmail.com",  // TODO: look up from User table
                    "Interview Reminder",
                    "Hey, don't forget your interview scheduled for tomorrow 3 PM!"
            );

            // 2. Mark delivered
            n.setSent(true);
            notificationRepository.save(n);

            // 3. Optional: also push into dashboard (future)
            System.out.println("In-app reminder: " + n.getMessage());
        }
    }

    public void createInterviewReminder(Application app) {
        Notification n = new Notification();
        n.setUserId(app.getUserId());
        n.setMessage("Interview Reminder - " + app.getCompanyName());
        n.setNotifyAt(app.getLastFollowUpDate().atStartOfDay().minusDays(1)); // remind 1 day before
        n.setSent(false);

        // Example: remind 1 day before interview date (assuming `lastFollowUpDate` is interview date for now)
        LocalDateTime eventDate = app.getLastFollowUpDate() != null 
                ? app.getLastFollowUpDate().atStartOfDay() 
                : LocalDateTime.now().plusDays(7); // fallback

        n.setNotifyAt(eventDate.minusDays(1)); // remind 1 day before
        n.setSent(false);

        notificationRepository.save(n);
    }
}

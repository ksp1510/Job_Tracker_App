package com.jobtracker.service;

import com.jobtracker.model.Application;
import com.jobtracker.model.Notification;
import com.jobtracker.repository.NotificationRepository;
import com.jobtracker.service.EmailService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.time.ZoneOffset;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public void createFollowUpReminder(Application app, int daysUntilFollowUp) {
        Notification n = new Notification();
        n.setUserId(app.getUserId());
        n.setApplicationId(app.getId());
        n.setMessage("Follow up on " + app.getCompanyName() + " - " + app.getJobTitle());
        n.setNotifyAt(Instant.now().plus(daysUntilFollowUp, ChronoUnit.DAYS));
        n.setSent(false);

        notificationRepository.save(n);
    }

    public Notification createNotification(Notification n) {
        // If reminderTime was passed as string, map it to notifyAt
        if (n.getNotifyAt() == null && n instanceof Notification) {
            
        }
        return notificationRepository.save(n);
    }

    public List<Notification> getUserNotifications(String userId) {
        return notificationRepository.findByUserId(userId);
    }

    public void processDueNotifications() {
        List<Notification> due = notificationRepository.findBySentFalseAndNotifyAtBefore(Instant.now());

        for (Notification n : due) {
            // 1. Send via SES (email)
            EmailService.sendEmail(
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
        n.setNotifyAt(app.getLastFollowUpDate().atStartOfDay().toInstant(ZoneOffset.UTC)); // remind 1 day before
        n.setSent(false);

        // Example: remind 1 day before interview date (assuming `lastFollowUpDate` is interview date for now)
        Instant eventDate = app.getLastFollowUpDate() != null 
                ? app.getLastFollowUpDate().atStartOfDay().toInstant(null) 
                : Instant.now().plus(7, ChronoUnit.DAYS); // fallback

        n.setNotifyAt(eventDate.minus(1, ChronoUnit.DAYS)); // remind 1 day before
        n.setSent(false);

        notificationRepository.save(n);
    }
}

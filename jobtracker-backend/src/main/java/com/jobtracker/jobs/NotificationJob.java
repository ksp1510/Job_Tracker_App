package com.jobtracker.jobs;

import com.jobtracker.service.NotificationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NotificationJob {

    private final NotificationService notificationService;

    public NotificationJob(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // Run every 1 minute
    /*
    @Scheduled(fixedRate = 60000)
    public void processNotifications() {
        try {
            System.out.println("🔄 [SCHEDULED JOB] Running notification job at " + java.time.LocalDateTime.now());
            notificationService.processDueNotifications();
        } catch (Exception e) {
            System.err.println("❌ Notification job failed: " + e.getMessage());
            e.printStackTrace();
        }
    }*/
}
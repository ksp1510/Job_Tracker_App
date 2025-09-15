package com.jobtracker.jobs;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.jobtracker.service.NotificationService;

@Component
public class NotificationJob {

    private final NotificationService notificationService;

    public NotificationJob(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Scheduled(fixedRate = 60000) // Every 1 minute
    public void processNotifications() {
        try {
            System.out.println("🔄 Running notification job...");
            notificationService.processDueNotifications();
        } catch (Exception e) {
            System.err.println("❌ Notification job failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
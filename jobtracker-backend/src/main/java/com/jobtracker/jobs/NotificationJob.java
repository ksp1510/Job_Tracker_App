package com.jobtracker.jobs;

import org.springframework.stereotype.Component;

@Component
public class NotificationJob {

    public NotificationJob() {
    }

    /*@Scheduled(fixedRate = 60000) // Every 1 minute
    public void processNotifications() {
        try {
            System.out.println("🔄 Running notification job...");
            notificationService.processDueNotifications();
        } catch (Exception e) {
            System.err.println("❌ Notification job failed: " + e.getMessage());
            e.printStackTrace();
        }
    }*/
}
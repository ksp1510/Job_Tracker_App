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

    // Run every 1 minute for demo (cron every hour in production)
    @Scheduled(fixedRate = 60000)
    public void run() {
        notificationService.processDueNotifications();
    }
}


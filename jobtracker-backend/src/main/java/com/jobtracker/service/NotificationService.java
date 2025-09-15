package com.jobtracker.service;

import com.jobtracker.model.Application;
import com.jobtracker.model.Notification;
import com.jobtracker.model.User;
import com.jobtracker.repository.NotificationRepository;
import com.jobtracker.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SesService sesService;

    public NotificationService(NotificationRepository notificationRepository, 
                             UserRepository userRepository,
                             SesService sesService) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.sesService = sesService;
    }

    /**
     * Automatically create follow-up reminder when application is created with APPLIED status
     * This is triggered from ApplicationService when new application is saved
     */
    public void createFollowUpReminder(Application app) {
        User user = getUserIfNotificationsEnabled(app.getUserId());
        if (user == null) return;

        Notification n = new Notification();
        n.setUserId(app.getUserId());
        n.setApplicationId(app.getId());
        n.setMessage("Time to follow up on your application at " + app.getCompanyName() + " for " + app.getJobTitle());
        n.setNotifyAt(Instant.now().plus(7, ChronoUnit.DAYS)); // 7 days from now
        n.setType(Notification.NotificationType.FOLLOW_UP);
        n.setChannel(Notification.Channel.IN_APP); // Follow-up is in-app only
        n.setSent(false);

        notificationRepository.save(n);
        System.out.println("✅ Follow-up reminder created for application: " + app.getCompanyName());
    }

    /**
     * Create interview reminder (user-initiated)
     */
    public Notification createInterviewReminder(String userId, String applicationId, 
                                              LocalDateTime interviewDate, String customMessage) {
        User user = getUserIfNotificationsEnabled(userId);
        if (user == null) {
            throw new RuntimeException("Notifications are disabled for this user");
        }

        Notification n = new Notification();
        n.setUserId(userId);
        n.setApplicationId(applicationId);
        n.setMessage(customMessage != null ? customMessage : "Interview reminder - Don't forget your interview tomorrow!");
        
        // Notify 1 day before interview
        n.setNotifyAt(interviewDate.minusDays(1).toInstant(ZoneOffset.UTC));
        n.setType(Notification.NotificationType.INTERVIEW);
        n.setChannel(user.isEmailNotificationsEnabled() ? 
                    Notification.Channel.EMAIL : Notification.Channel.IN_APP);
        n.setSent(false);

        return notificationRepository.save(n);
    }

    /**
     * Create assessment deadline reminder (user-initiated)
     */
    public Notification createAssessmentDeadlineReminder(String userId, String applicationId, 
                                                       LocalDateTime assessmentDeadline, String customMessage) {
        User user = getUserIfNotificationsEnabled(userId);
        if (user == null) {
            throw new RuntimeException("Notifications are disabled for this user");
        }

        Notification n = new Notification();
        n.setUserId(userId);
        n.setApplicationId(applicationId);
        n.setMessage(customMessage != null ? customMessage : "Assessment deadline reminder - Complete your assessment soon!");
        
        // Notify 1 day before deadline
        n.setNotifyAt(assessmentDeadline.minusDays(1).toInstant(ZoneOffset.UTC));
        n.setType(Notification.NotificationType.DEADLINE);
        n.setChannel(user.isEmailNotificationsEnabled() ? 
                    Notification.Channel.EMAIL : Notification.Channel.IN_APP);
        n.setSent(false);

        return notificationRepository.save(n);
    }

    /**
     * Generic notification creation (for manual notifications)
     */
    public Notification createNotification(Notification n) {
        User user = getUserIfNotificationsEnabled(n.getUserId());
        if (user == null) {
            throw new RuntimeException("Notifications are disabled for this user");
        }
        return notificationRepository.save(n);
    }

    /**
     * Get all notifications for a user
     */
    public List<Notification> getUserNotifications(String userId) {
        return notificationRepository.findByUserId(userId);
    }

    /**
     * Process due notifications (called by scheduled job)
     */
    public void processDueNotifications() {
        List<Notification> due = notificationRepository.findBySentFalseAndNotifyAtBefore(Instant.now());
        System.out.println("🔔 Processing " + due.size() + " due notifications...");

        for (Notification n : due) {
            try {
                User user = userRepository.findById(n.getUserId()).orElse(null);
                if (user == null || !user.isNotificationEnabled()) {
                    // Mark as sent but skip processing
                    n.setSent(true);
                    notificationRepository.save(n);
                    continue;
                }

                // Send notification based on channel
                if (n.getChannel() == Notification.Channel.EMAIL && user.isEmailNotificationsEnabled()) {
                    sendEmailNotification(user, n);
                }
                
                // Always create in-app notification (if user has in-app enabled)
                if (user.isInAppNotificationsEnabled()) {
                    System.out.println("📱 In-app notification for " + user.getEmail() + ": " + n.getMessage());
                }

                // Mark as sent
                n.setSent(true);
                notificationRepository.save(n);
                
                System.out.println("✅ Notification sent to " + user.getEmail() + ": " + n.getMessage());
                
            } catch (Exception e) {
                System.err.println("❌ Failed to send notification: " + e.getMessage());
                // Don't mark as sent so it can be retried
            }
        }
    }

    /**
     * Update user notification preferences
     */
    public User updateNotificationPreferences(String userId, boolean notificationsEnabled, 
                                            boolean emailEnabled, boolean inAppEnabled) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setNotificationEnabled(notificationsEnabled);
        user.setEmailNotificationsEnabled(emailEnabled);
        user.setInAppNotificationsEnabled(inAppEnabled);
        
        return userRepository.save(user);
    }

    /**
     * Delete notification (user action)
     */
    public void deleteNotification(String userId, String notificationId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        
        if (!n.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized to delete this notification");
        }
        
        notificationRepository.deleteById(notificationId);
    }

    // Helper methods
    private User getUserIfNotificationsEnabled(String userId) {
        User user = userRepository.findById(userId).orElse(null);
        return (user != null && user.isNotificationEnabled()) ? user : null;
    }

    private void sendEmailNotification(User user, Notification n) {
        try {
            String subject = getEmailSubject(n.getType());
            sesService.sendEmail(user.getEmail(), subject, n.getMessage());
            System.out.println("📧 Email sent to " + user.getEmail());
        } catch (Exception e) {
            System.err.println("❌ Failed to send email to " + user.getEmail() + ": " + e.getMessage());
            throw e;
        }
    }

    private String getEmailSubject(Notification.NotificationType type) {
        switch (type) {
            case FOLLOW_UP:
                return "🔔 Follow-up Reminder - Job Application";
            case INTERVIEW:
                return "📅 Interview Reminder";
            case DEADLINE:
                return "⏰ Assessment Deadline Reminder";
            default:
                return "🔔 Job Tracker Notification";
        }
    }
}
package com.jobtracker.service;

import com.jobtracker.model.Application;
import com.jobtracker.model.Notification;
import com.jobtracker.model.Status;
import com.jobtracker.model.User;
import com.jobtracker.repository.ApplicationRepository;
import com.jobtracker.repository.NotificationRepository;
import com.jobtracker.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final SesService sesService;

    public NotificationService(NotificationRepository notificationRepository, 
                             UserRepository userRepository,
                             ApplicationRepository applicationRepository,
                             SesService sesService) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.applicationRepository = applicationRepository;
        this.sesService = sesService;
    }

    /**
     * Create follow-up reminder when application is created with APPLIED status
     * Reminder will be sent 7 days later, ONLY if status is still APPLIED
     */
    public void createFollowUpReminder(Application app) {
        User user = getUserIfNotificationsEnabled(app.getUserId());
        if (user == null) return;
    
        Notification n = new Notification();
        n.setUserId(app.getUserId());
        n.setApplicationId(app.getId());
        n.setMessage("Time to follow up on your application at " + app.getCompanyName() + " for " + app.getJobTitle());
        
        // FIXED: Convert Instant to LocalDateTime
        LocalDateTime notifyTime = LocalDateTime.now().plusDays(7);
        n.setNotifyAt(notifyTime);
        
        n.setType(Notification.NotificationType.FOLLOW_UP);
        n.setChannel(Notification.Channel.IN_APP);
        n.setSent(false);
    
        notificationRepository.save(n);
        System.out.println("✅ Follow-up reminder created for application: " + app.getCompanyName() + " (notify at: " + n.getNotifyAt() + ")");
    }

    /**
     * Create interview reminder - 24 hours before interview
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
        n.setMessage(customMessage != null ? customMessage : 
            "Reminder: Your interview is tomorrow! Good luck!");
        
        // FIXED: Use LocalDateTime directly, no conversion to Instant
        n.setNotifyAt(interviewDate.minusDays(1));
        n.setType(Notification.NotificationType.INTERVIEW);
        n.setChannel(user.isEmailNotificationsEnabled() ? 
                    Notification.Channel.EMAIL : Notification.Channel.IN_APP);
        n.setSent(false);

        Notification saved = notificationRepository.save(n);
        System.out.println("✅ Interview reminder created (notify 24hrs before): " + saved.getNotifyAt());
        return saved;
    }

    /**
     * Create assessment deadline reminder - 24 hours before deadline
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
        n.setMessage(customMessage != null ? customMessage : 
            "Reminder: Your assessment deadline is tomorrow!");
        
        // FIXED: Use LocalDateTime directly, no conversion to Instant
        n.setNotifyAt(assessmentDeadline.minusDays(1));
        n.setType(Notification.NotificationType.DEADLINE);
        n.setChannel(user.isEmailNotificationsEnabled() ? 
                    Notification.Channel.EMAIL : Notification.Channel.IN_APP);
        n.setSent(false);

        Notification saved = notificationRepository.save(n);
        System.out.println("✅ Assessment reminder created (notify 24hrs before): " + saved.getNotifyAt());
        return saved;
    }

    /**
     * Generic notification creation
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
     * Process due notifications - CHECK STATUS BEFORE SENDING FOLLOW-UP
     */
    public void processDueNotifications() {
        // FIXED: Use LocalDateTime.now() instead of Instant.now()
        List<Notification> due = notificationRepository.findBySentFalseAndNotifyAtBefore(LocalDateTime.now());
        System.out.println("🔔 Processing " + due.size() + " due notifications...");
    
        for (Notification n : due) {
            try {
                User user = userRepository.findById(n.getUserId()).orElse(null);
                if (user == null || !user.isNotificationEnabled()) {
                    n.setSent(true);
                    notificationRepository.save(n);
                    continue;
                }
    
                // CRITICAL: Check if follow-up notification should still be sent
                if (n.getType() == Notification.NotificationType.FOLLOW_UP) {
                    Application app = applicationRepository.findById(n.getApplicationId()).orElse(null);
                    
                    // Only send follow-up if status is still APPLIED
                    if (app == null || app.getStatus() != Status.APPLIED) {
                        System.out.println("⏭️ Skipping follow-up: status changed from APPLIED");
                        n.setSent(true); // Mark as sent to avoid resending
                        notificationRepository.save(n);
                        continue;
                    }
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
                e.printStackTrace();
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
        
        User updatedUser = userRepository.save(user);
        System.out.println("✅ Notification preferences updated for user: " + user.getEmail());
        
        return updatedUser;
    }

    /**
     * Delete notification
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
                return "📅 Interview Reminder - Tomorrow!";
            case DEADLINE:
                return "⏰ Assessment Deadline - Tomorrow!";
            default:
                return "🔔 Job Tracker Notification";
        }
    }
}
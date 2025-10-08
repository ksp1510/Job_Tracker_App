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
     */
    public void createFollowUpReminder(Application app) {
        User user = getUserIfNotificationsEnabled(app.getUserId());
        if (user == null) return;
    
        Notification n = new Notification();
        n.setUserId(app.getUserId());
        n.setApplicationId(app.getId());
        n.setMessage("Time to follow up on your application at " + app.getCompanyName() + " for " + app.getJobTitle());
        
        LocalDateTime notifyTime = LocalDateTime.now().plusDays(7);
        n.setNotifyAt(notifyTime);
        
        n.setType(Notification.NotificationType.FOLLOW_UP);
        n.setChannel(user.isEmailNotificationsEnabled() ? 
                    Notification.Channel.EMAIL : Notification.Channel.IN_APP);
        n.setSent(false);
    
        notificationRepository.save(n);
        System.out.println("✅ Follow-up reminder created for application: " + app.getCompanyName());
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

        Application app = applicationRepository.findById(applicationId).orElse(null);
        String message = customMessage != null ? customMessage : 
            String.format("Reminder: Interview tomorrow for %s at %s! Good luck!", 
                app != null ? app.getJobTitle() : "position",
                app != null ? app.getCompanyName() : "company");

        Notification n = new Notification();
        n.setUserId(userId);
        n.setApplicationId(applicationId);
        n.setMessage(message);
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

        Application app = applicationRepository.findById(applicationId).orElse(null);
        String message = customMessage != null ? customMessage : 
            String.format("Reminder: Assessment deadline tomorrow for %s at %s!", 
                app != null ? app.getJobTitle() : "position",
                app != null ? app.getCompanyName() : "company");

        Notification n = new Notification();
        n.setUserId(userId);
        n.setApplicationId(applicationId);
        n.setMessage(message);
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
     * ENHANCED: Send emails via AWS SES
     */
    public void processDueNotifications() {
        LocalDateTime now = LocalDateTime.now();
        List<Notification> due = notificationRepository
            .findBySentFalseAndNotifyAtBefore(now);

        System.out.println("🔔 Processing " + due.size() + " due notifications...");
    
        for (Notification n : due) {
            try {

                // Skip if notification time is in the future
                if (n.getNotifyAt().isAfter(now)) {
                    System.out.println("⏭️ Skipping notification: notifyAt is in the future: " + n.getNotifyAt());
                    continue;
                }

                User user = userRepository.findById(n.getUserId()).orElse(null);
                if (user == null || !user.isNotificationEnabled()) {
                    System.out.println("⏭️ Skipping notification: user is disabled");
                    n.setSent(true);
                    notificationRepository.save(n);
                    continue;
                }
    
                // Check if follow-up notification should still be sent
                if (n.getType() == Notification.NotificationType.FOLLOW_UP) {
                    Application app = applicationRepository.findById(n.getApplicationId()).orElse(null);
                    
                    if (app == null || app.getStatus() != Status.APPLIED) {
                        System.out.println("⏭️ Skipping follow-up: status changed from APPLIED");
                        n.setSent(true);
                        notificationRepository.save(n);
                        continue;
                    }
                }
    
                // Get application details for email
                Application app = n.getApplicationId() != null ? 
                    applicationRepository.findById(n.getApplicationId()).orElse(null) : null;
    
                // Send notification based on channel
                if (n.getChannel() == Notification.Channel.EMAIL && user.isEmailNotificationsEnabled()) {
                    sendEmailNotification(user, n, app);
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

    /**
     * ENHANCED: Send email notification with application details
     */
    private void sendEmailNotification(User user, Notification notification, Application app) {
        try {
            String subject = getEmailSubject(notification.getType());
            String htmlBody = buildEmailHtml(user, notification, app);
            
            sesService.sendHtmlEmail(user.getEmail(), subject, htmlBody);
            System.out.println("📧 Email sent to " + user.getEmail());
        } catch (Exception e) {
            System.err.println("❌ Failed to send email to " + user.getEmail() + ": " + e.getMessage());
            throw e;
        }
    }

    /**
     * Build HTML email with application details
     */
    private String buildEmailHtml(User user, Notification notification, Application app) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html><head><style>");
        html.append("body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }");
        html.append(".container { max-width: 600px; margin: 0 auto; padding: 20px; }");
        html.append(".header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }");
        html.append(".content { background: #f8f9fa; padding: 30px; border-radius: 0 0 10px 10px; }");
        html.append(".app-info { background: white; padding: 20px; margin: 20px 0; border-radius: 8px; border-left: 4px solid #667eea; }");
        html.append(".app-title { font-size: 18px; font-weight: bold; color: #667eea; margin-bottom: 5px; }");
        html.append(".app-company { font-size: 16px; color: #555; margin-bottom: 10px; }");
        html.append(".message-box { background: white; padding: 20px; margin: 20px 0; border-radius: 8px; }");
        html.append(".footer { text-align: center; color: #888; font-size: 12px; margin-top: 20px; }");
        html.append(".button { display: inline-block; padding: 12px 24px; background: #667eea; color: white; text-decoration: none; border-radius: 5px; margin-top: 15px; }");
        html.append("</style></head><body>");
        html.append("<div class='container'>");
        html.append("<div class='header'>");
        html.append("<h1>🔔 JobTracker Reminder</h1>");
        html.append("</div>");
        html.append("<div class='content'>");
        html.append("<p>Hi ").append(user.getFirstName()).append(",</p>");
        
        // Application details if available
        if (app != null) {
            html.append("<div class='app-info'>");
            html.append("<div class='app-title'>📋 ").append(app.getJobTitle()).append("</div>");
            html.append("<div class='app-company'>🏢 ").append(app.getCompanyName()).append("</div>");
            if (app.getJobLocation() != null) {
                html.append("<div>📍 ").append(app.getJobLocation()).append("</div>");
            }
            html.append("</div>");
        }
        
        // Message
        html.append("<div class='message-box'>");
        html.append("<p><strong>").append(notification.getMessage()).append("</strong></p>");
        html.append("<p style='color: #666;'>Scheduled for: ").append(notification.getNotifyAt()).append("</p>");
        html.append("</div>");
        
        // Action button
        html.append("<p style='text-align: center;'>");
        html.append("<a href='http://localhost:3000/applications' class='button'>View Application</a>");
        html.append("</p>");
        
        html.append("<div class='footer'>");
        html.append("<p>This is an automated reminder from JobTracker</p>");
        html.append("<p>You can manage your notification preferences in settings</p>");
        html.append("</div>");
        html.append("</div>");
        html.append("</div>");
        html.append("</body></html>");
        
        return html.toString();
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
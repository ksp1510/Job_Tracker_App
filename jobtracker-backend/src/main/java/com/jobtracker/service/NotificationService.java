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

        // FIXED: Calculate notification time (7 days from now)
        LocalDateTime appliedDateTime = app.getCreatedAt() != null ?
            LocalDateTime.parse(app.getCreatedAt() + "T00:00:00") :
            LocalDateTime.now();
        
        LocalDateTime notifyTime = appliedDateTime.plusDays(7);

        System.out.println("📅 Application created on: " + appliedDateTime);
        System.out.println("🔔 Follow-up reminder will be sent at: " + notifyTime);
       
        Notification n = new Notification();
        n.setUserId(app.getUserId());
        n.setApplicationId(app.getId());
        n.setMessage("Time to follow up on your application at " + app.getCompanyName() + " for " + app.getJobTitle());
        n.setNotifyAt(notifyTime);
        n.setType(Notification.NotificationType.FOLLOW_UP);
        n.setChannels(List.of(user.isEmailNotificationsEnabled() ? 
                    Notification.Channel.EMAIL : Notification.Channel.IN_APP));
        n.setSent(false);
        n.setRead(false);
        n.setCreatedAt(LocalDateTime.now());

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
            System.out.println("Notifications are disabled for this user");
            throw new RuntimeException("Notifications are disabled for this user");
        }

        Application app = applicationRepository.findById(applicationId).orElse(null);
        
        LocalDateTime notifyAt = interviewDate.minusDays(1);

        System.out.println("📅 Interview scheduled for: " + interviewDate);
        System.out.println("🔔 Interview reminder will be sent at: " + notifyAt);
        
        String message = customMessage != null ? customMessage : 
            String.format("Reminder: Interview tomorrow for %s at %s! Good luck!", 
                app != null ? app.getJobTitle() : "position",
                app != null ? app.getCompanyName() : "company");

        Notification n = new Notification();
        n.setUserId(userId);
        n.setApplicationId(applicationId);
        n.setMessage(message);
        n.setNotifyAt(notifyAt);
        n.setType(Notification.NotificationType.INTERVIEW);
        n.setChannels(List.of(user.isEmailNotificationsEnabled() ? 
                    Notification.Channel.EMAIL : Notification.Channel.IN_APP));
        n.setSent(false);
        n.setRead(false);
        n.setCreatedAt(LocalDateTime.now());

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
            System.out.println("⚠️ User notifications disabled or user not found for: " + userId);
            throw new RuntimeException("Notifications are disabled for this user");
        }

        Application app = applicationRepository.findById(applicationId).orElse(null);

        LocalDateTime notifyAt = assessmentDeadline.minusDays(1);

        System.out.println("📅 Assessment deadline scheduled for: " + assessmentDeadline);
        System.out.println("🔔 Assessment reminder will be sent at: " + notifyAt);
        
        String message = customMessage != null ? customMessage : 
            String.format("Reminder: Assessment deadline tomorrow for %s at %s!", 
                app != null ? app.getJobTitle() : "position",
                app != null ? app.getCompanyName() : "company");

        Notification n = new Notification();
        n.setUserId(userId);
        n.setApplicationId(applicationId);
        n.setMessage(message);
        n.setNotifyAt(notifyAt);
        n.setType(Notification.NotificationType.DEADLINE);
        n.setChannels(List.of(user.isEmailNotificationsEnabled() ? 
                    Notification.Channel.EMAIL : Notification.Channel.IN_APP));
        n.setSent(false);
        n.setRead(false);
        n.setCreatedAt(LocalDateTime.now());

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
            System.out.println("⚠️ User notifications disabled or user not found for: " + n.getUserId());
            throw new RuntimeException("Notifications are disabled for this user");
        }

        if (n.getCreatedAt() == null) {
            n.setCreatedAt(LocalDateTime.now());
        }

        if (n.isSent() == false) {
            n.setSent(false);
        }

        if (n.isRead() == false) {
            n.setRead(false);
        }

        if (n.getType() == null) {
            n.setType(Notification.NotificationType.CUSTOM);
        }

        if (n.getChannels() == null) {
            n.setChannels(List.of(user.isEmailNotificationsEnabled() ? 
                        Notification.Channel.EMAIL : Notification.Channel.IN_APP));
        }

        System.out.println("📅 Generic notification scheduled for: " + n.getNotifyAt());
        
        Notification saved = notificationRepository.save(n);
        
        System.out.println("✅ Notification created - will notify at: " + saved.getNotifyAt());
        
        return saved;
    }

    /**
     * Get all notifications for a user
     */
    public List<Notification> getUserNotifications(String userId) {
        return notificationRepository.findByUserIdOrderByNotifyAtDesc(userId);
    }

    /**
     * Process due notifications - CHECK STATUS BEFORE SENDING FOLLOW-UP
     * ENHANCED: Send emails via AWS SES
     */
    public void processDueNotifications() {
        LocalDateTime now = LocalDateTime.now();
        List<Notification> due = notificationRepository
            .findBySentFalseAndNotifyAtBefore(now);

        System.out.println("🔔 ========================================");
        System.out.println("🔔 Processing notifications at: " + now);
        System.out.println("🔔 Found " + due.size() + " due notifications");
        System.out.println("🔔 ========================================");
    
        for (Notification n : due) {
            try {

                System.out.println("\n📬 Processing notification ID: " + n.getId());
                System.out.println("   Type: " + n.getType());
                System.out.println("   Scheduled for: " + n.getNotifyAt());
                System.out.println("   Current time: " + now);

                // Skip if notification time is in the future
                if (n.getNotifyAt().isAfter(now)) {
                    System.out.println("⏭️ Skipping notification: notifyAt is in the future: " + n.getNotifyAt());
                    continue;
                }

                User user = userRepository.findById(n.getUserId()).orElse(null);
                if (user == null) {
                    System.out.println("❌ User not found for notification ID: " + n.getId());
                    n.setSent(true);
                    notificationRepository.save(n);
                    continue;
                }

                if (!user.isNotificationEnabled()) {
                    System.out.println("⚠️ User notifications disabled for notification ID: " + n.getId());
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
                
                boolean emailSent = false;
                
                // Send notification based on channel
                if (n.getChannels().contains(Notification.Channel.EMAIL) && user.isEmailNotificationsEnabled()) {
                    System.out.println("📧 Sending email notification to " + user.getEmail() + ": " + n.getMessage());
                    try {
                        sendEmailNotification(user, n, app);
                        emailSent = true;
                        System.out.println("✅ Email notification sent successfully to " + user.getEmail());
                    } catch (Exception emailError) {
                        System.err.println("❌ Failed to send email: " + emailError.getMessage());
                        emailError.printStackTrace();
                        continue;
                    }
                } else {
                    System.out.println("⚠️ Email notifications disabled for user: " + user.getEmail());
                    emailSent = true;
                }
                
                // Always create in-app notification (if user has in-app enabled)
                if (user.isInAppNotificationsEnabled()) {
                    System.out.println("📱 In-app notification enabled for " + user.getEmail());
                } else {
                    System.out.println("⚠️ In-app notifications disabled for user: " + user.getEmail());
                }
    
                // Mark as sent
                if (emailSent) {
                    n.setSent(true);
                    notificationRepository.save(n);
                    System.out.println("✅ Notification marked as sent: " + n.getId());
                }
                
                System.out.println("✅ Notification processed successfully");
                System.out.println("   User: " + user.getEmail());
                System.out.println("   Notification: " + n.getMessage());
                
            } catch (Exception e) {
                System.err.println("❌ Failed to process notification ID: " + n.getId());
                System.err.println("❌ Error: " + e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("\n🔔 ========================================");
        System.out.println("🔔 Notification processing complete");
        System.out.println("🔔 ========================================\n");
    }

    /**
     * Update user notification preferences
     */
    public User updateNotificationPreferences(String userId, boolean notificationsEnabled, 
                                            boolean emailEnabled, boolean inAppEnabled) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        System.out.println("⚙️ Updating notification preferences for user: " + user.getEmail());
        System.out.println("   Notifications enabled: " + notificationsEnabled);
        System.out.println("   Email enabled: " + emailEnabled);
        System.out.println("   In-app enabled: " + inAppEnabled);
        
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
        System.out.println("🗑️ Notification deleted: " + notificationId);
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
            
            System.out.println("📧 Preparing to send email:");
            System.out.println("   To: " + user.getEmail());
            System.out.println("   Subject: " + subject);
            
            sesService.sendHtmlEmail(user.getEmail(), subject, htmlBody);
            System.out.println("✅ Email sent successfully to " + user.getEmail());
        } catch (Exception e) {
            System.err.println("❌ Failed to send email to " + user.getEmail());
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
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
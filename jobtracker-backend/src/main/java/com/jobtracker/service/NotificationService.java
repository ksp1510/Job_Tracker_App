package com.jobtracker.service;

import com.jobtracker.model.Application;
import com.jobtracker.model.Notification;
import com.jobtracker.model.NotificationPreference;
import com.jobtracker.model.Status;
import com.jobtracker.model.User;
import com.jobtracker.repository.ApplicationRepository;
import com.jobtracker.repository.NotificationRepository;
import com.jobtracker.repository.NotificationPreferenceRepository;
import com.jobtracker.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final SesService sesService;

    public NotificationService(NotificationRepository notificationRepository, 
                             UserRepository userRepository,
                             ApplicationRepository applicationRepository,
                             NotificationPreferenceRepository notificationPreferenceRepository,
                             SesService sesService) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.applicationRepository = applicationRepository;
        this.notificationPreferenceRepository = notificationPreferenceRepository;
        this.sesService = sesService;
    }

    /**
     * Create follow-up reminder when application is created with APPLIED status
     */
    public void createFollowUpReminder(Application app) {
        LocalDateTime appliedLocal; 
        if (app.getCreatedAt() instanceof LocalDateTime createdAt) {
            appliedLocal = createdAt;
        } else {
            String createdAtStr = String.valueOf(app.getCreatedAt());
            appliedLocal = LocalDateTime.parse(createdAtStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
        
        LocalDateTime notifyLocal = appliedLocal.plusDays(7);

        LocalDateTime notifyAtUTC = notifyLocal.atZone(ZoneId.systemDefault())
            .withZoneSameInstant(ZoneOffset.UTC)
            .toLocalDateTime();

        System.out.println("📅 Application created on: " + appliedLocal);
        System.out.println("🔔 Follow-up reminder will be sent at: " + notifyAtUTC);
       
        Notification n = new Notification();
        n.setUserId(app.getUserId());
        n.setApplicationId(app.getId());
        n.setMessage("Time to follow up on your application at " + app.getCompanyName() + " for " + app.getJobTitle());
        n.setNotifyAt(notifyAtUTC);
        n.setType(Notification.NotificationType.FOLLOW_UP);
        n.setSent(false);
        n.setRead(false);
        n.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));

        notificationRepository.save(n);
        System.out.println("✅ Follow-up reminder created for application: " + app.getCompanyName());
    }

    /**
     * Create interview reminder - 24 hours before interview
     */
    public Notification createInterviewReminder(String userId, String applicationId,
                                          LocalDateTime interviewDate, String customMessage) {

        Application app = applicationRepository.findById(applicationId).orElse(null);
        
        LocalDateTime interviewUTC = interviewDate.atZone(ZoneId.systemDefault())
            .withZoneSameInstant(ZoneOffset.UTC)
            .toLocalDateTime();
        
        LocalDateTime notifyAtUTC = interviewUTC.minusDays(1);

        System.out.println("📅 Interview scheduled for: " + interviewDate);
        System.out.println("🔔 Interview reminder will be sent at: " + notifyAtUTC);
        
        String message = customMessage != null ? customMessage : 
            String.format("Reminder: Interview tomorrow for %s at %s! Good luck!", 
                app != null ? app.getJobTitle() : "position",
                app != null ? app.getCompanyName() : "company");

        Notification n = new Notification();
        n.setUserId(userId);
        n.setApplicationId(applicationId);
        n.setMessage(message);
        n.setNotifyAt(notifyAtUTC);
        n.setType(Notification.NotificationType.INTERVIEW);
        n.setSent(false);
        n.setRead(false);
        n.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));

        Notification saved = notificationRepository.save(n);
        System.out.println("✅ Interview reminder created (notify 24hrs before): " + saved.getNotifyAt());
        return saved;
    }

    /**
     * Create assessment deadline reminder - 24 hours before deadline
     */
    public Notification createAssessmentDeadlineReminder(String userId, String applicationId,
                                                    LocalDateTime assessmentDeadline, String customMessage) {
        Application app = applicationRepository.findById(applicationId).orElse(null);

        LocalDateTime assessmentUTC = assessmentDeadline.atZone(ZoneId.systemDefault())
            .withZoneSameInstant(ZoneOffset.UTC)
            .toLocalDateTime();
        
        LocalDateTime notifyAtUTC = assessmentUTC.minusDays(1);

        System.out.println("📅 Assessment deadline scheduled for: " + assessmentDeadline);
        System.out.println("🔔 Assessment reminder will be sent at: " + notifyAtUTC);
        
        String message = customMessage != null ? customMessage : 
            String.format("Reminder: Assessment deadline tomorrow for %s at %s!", 
                app != null ? app.getJobTitle() : "position",
                app != null ? app.getCompanyName() : "company");

        Notification n = new Notification();
        n.setUserId(userId);
        n.setApplicationId(applicationId);
        n.setMessage(message);
        n.setNotifyAt(notifyAtUTC);
        n.setType(Notification.NotificationType.DEADLINE);
        n.setSent(false);
        n.setRead(false);
        n.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));

        Notification saved = notificationRepository.save(n);
        System.out.println("✅ Assessment reminder created (notify 24hrs before): " + saved.getNotifyAt());
        return saved;
    }

    /**
     * Generic notification creation
     */
    public Notification createNotification(Notification n) {
        if (n.getCreatedAt() == null) {
            n.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
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
        LocalDateTime nowUTC = LocalDateTime.now(ZoneId.of("UTC"));
        List<Notification> due = notificationRepository
            .findBySentFalseAndNotifyAtBefore(nowUTC);

        System.out.println("🔔 ========================================");
        System.out.println("🔔 Processing notifications at: " + nowUTC);
        System.out.println("🔔 Found " + due.size() + " due notifications");
        System.out.println("🔔 ========================================");
    
        for (Notification n : due) {
            try {

                System.out.println("\n📬 Processing notification ID: " + n.getId());
                System.out.println("   Type: " + n.getType());
                System.out.println("   Scheduled for: " + n.getNotifyAt());
                System.out.println("   Current time: " + nowUTC);

                // Skip if notification time is in the future
                if (n.getNotifyAt().isAfter(nowUTC)) {
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

                /*if (!user.isNotificationEnabled()) {
                    System.out.println("⚠️ User notifications disabled for notification ID: " + n.getId());
                    n.setSent(true);
                    notificationRepository.save(n);
                    continue;
                }*/

                NotificationPreference pref = notificationPreferenceRepository.findByUserId(user.getUserId())
                        .orElse(new NotificationPreference(user.getUserId(), true, true, LocalDateTime.now()));

                if (!pref.isInAppEnabled() && !pref.isEmailEnabled()) {
                    System.out.println("⚠️ All notifications disabled for user: " + user.getEmail());
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

                // ✅ Apply user’s persistent preferences
                if (pref.isEmailEnabled()) {
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
                    emailSent = true; // mark as handled even if not sent
                }
                
                // ✅ Always keep in-app notifications on
                if (pref.isInAppEnabled()) {
                    saveInAppNotification(user, n, app);
                    System.out.println("📱 In-app notification saved for " + user.getEmail());
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

    private void saveInAppNotification(User user, Notification notification, Application app) {
        try {
            // Clone or adapt the existing notification for in-app display
            Notification inApp = new Notification();
            inApp.setUserId(user.getUserId());
            inApp.setApplicationId(notification.getApplicationId());
            inApp.setMessage(notification.getMessage());
            inApp.setType(notification.getType());
            inApp.setNotifyAt(notification.getNotifyAt());
            inApp.setCreatedAt(LocalDateTime.now());
            inApp.setSent(true); // mark delivered immediately for in-app
    
            notificationRepository.save(inApp);
    
            System.out.println("💾 In-app notification persisted for user: " + user.getEmail());
        } catch (Exception e) {
            System.err.println("❌ Failed to save in-app notification: " + e.getMessage());
            e.printStackTrace();
        }
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
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm a").withZone(ZoneId.systemDefault());
        String formattedTime = formatter.format(notification.getNotifyAt().atZone(ZoneOffset.UTC));
        
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
        html.append("<p style='color: #666;'>Scheduled for: ").append(formattedTime).append("</p>");
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
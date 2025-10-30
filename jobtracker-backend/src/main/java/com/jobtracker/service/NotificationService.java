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

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

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

    private static final Map<String,String> TZ_ABBR = Map.of(
    "EST","America/Toronto", "EDT","America/Toronto",
    "PST","America/Los_Angeles", "PDT","America/Los_Angeles",
    "CST","America/Chicago", "CDT","America/Chicago",
    "IST","Asia/Kolkata"
    );

    private ZoneId getUserZone(String userId) {
        String id = userRepository.findById(userId)
            .map(User::getTimezone).orElse(null);

        if (id == null || id.isBlank()) return ZoneId.of("UTC");
        String canonical = TZ_ABBR.getOrDefault(id.trim(), id.trim());
        try {
            return ZoneId.of(canonical); // IANA only
        } catch (Exception e) {
            // log once
            return ZoneId.of("UTC");
        }
    }

    /**
     * Create follow-up reminder when application is created with APPLIED status
     */
    public void createFollowUpReminder(Application app) {
        try {
            // Validate inputs
            if (app == null) {
                System.err.println("❌ Cannot create follow-up reminder: application is null");
                return;
            }
            
            if (app.getId() == null) {
                System.err.println("❌ Cannot create follow-up reminder: application ID is null");
                return;
            }
            
            if (app.getUserId() == null) {
                System.err.println("❌ Cannot create follow-up reminder: user ID is null");
                return;
            }
            
            // Get created time (should be Instant)
            Instant createdAtInstant = app.getCreatedAt();
            if (createdAtInstant == null) {
                System.err.println("⚠️ CreatedAt is null, using current time");
                createdAtInstant = Instant.now();
            }
            
            // Get user timezone
            ZoneId userZone = getUserZone(app.getUserId());
            
            // Calculate notification time: 7 days after application in user's timezone
            ZonedDateTime createdInUserTz = createdAtInstant.atZone(userZone);
            ZonedDateTime notifyAtUserTz = createdInUserTz.plusDays(7);
            
            // Convert to UTC for storage
            Instant notifyAtInstant = notifyAtUserTz.toInstant();
            LocalDateTime notifyAtUTC = LocalDateTime.ofInstant(notifyAtInstant, ZoneOffset.UTC);
            
            // Create notification
            Notification n = new Notification();
            n.setUserId(app.getUserId());
            n.setApplicationId(app.getId());
            n.setMessage(String.format(
                "Time to follow up on your application at %s for %s",
                app.getCompanyName(),
                app.getJobTitle()
            ));
            n.setNotifyAt(notifyAtUTC);
            n.setType(Notification.NotificationType.FOLLOW_UP);
            n.setSent(false);
            n.setRead(false);
            n.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
            
            // Save notification
            notificationRepository.save(n);
            
            // Log success
            System.out.println("✅ Follow-up reminder created:");
            System.out.println("   Application ID: " + app.getId());
            System.out.println("   Company: " + app.getCompanyName());
            System.out.println("   Created on: " + createdAtInstant);
            System.out.println("   Notify at (UTC): " + notifyAtUTC);
            System.out.println("   Notify at (User TZ): " + notifyAtUserTz);
            
        } catch (Exception e) {
            System.err.println("❌ Error creating follow-up reminder for application " + 
                (app != null ? app.getId() : "unknown") + ": " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to create follow-up reminder", e);
        }
    }

    /**
     * Create interview reminder - 24 hours before interview
     * @param interviewDateTimeStr ISO 8601 datetime string with timezone (e.g., "2025-10-28T21:50:00-04:00")
     */
    public Notification createInterviewReminder(String userId, String applicationId,
                                            String interviewDateTimeStr, String customMessage) {
        Application app = applicationRepository.findById(applicationId).orElse(null);
        
        // Parse the ISO 8601 string with timezone
        ZonedDateTime interviewDateTime = ZonedDateTime.parse(interviewDateTimeStr);
        
        // Convert to UTC for storage
        ZonedDateTime interviewUtc = interviewDateTime.withZoneSameInstant(ZoneOffset.UTC);
        LocalDateTime eventDateUtc = interviewUtc.toLocalDateTime();
        LocalDateTime notifyAtUtc = interviewUtc.minusDays(1).toLocalDateTime();
        
        // Validate notification time hasn't passed
        if (notifyAtUtc.isBefore(LocalDateTime.now(ZoneOffset.UTC))) {
            throw new IllegalArgumentException(
                "Interview date is too soon - notification time has already passed. " +
                "Please schedule the interview at least 24 hours in advance."
            );
        }
        
        String message = (customMessage != null && !customMessage.isBlank())
            ? customMessage
            : String.format("Reminder: Interview tomorrow for %s at %s!",
                app != null ? app.getJobTitle() : "position",
                app != null ? app.getCompanyName() : "company");
        
        Notification n = new Notification();
        n.setUserId(userId);
        n.setApplicationId(applicationId);
        n.setType(Notification.NotificationType.INTERVIEW);
        n.setMessage(message);
        n.setEventDate(eventDateUtc);
        n.setNotifyAt(notifyAtUtc);
        n.setSent(false);
        n.setRead(false);
        n.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        
        return notificationRepository.save(n);
    }

    /**
     * Create assessment deadline reminder
     * @param deadlineDateTimeStr ISO 8601 datetime string with timezone
     */
    public Notification createAssessmentDeadlineReminder(String userId, String applicationId,
                                            String deadlineDateTimeStr, String customMessage) {
        Application app = applicationRepository.findById(applicationId).orElse(null);
        
        // Parse the ISO 8601 string with timezone
        ZonedDateTime deadlineDateTime = ZonedDateTime.parse(deadlineDateTimeStr);
        
        // Convert to UTC for storage
        ZonedDateTime deadlineUtc = deadlineDateTime.withZoneSameInstant(ZoneOffset.UTC);
        LocalDateTime eventDateUtc = deadlineUtc.toLocalDateTime();
        LocalDateTime notifyAtUtc = deadlineUtc.minusDays(1).toLocalDateTime();
        
        // Validate notification time hasn't passed
        if (notifyAtUtc.isBefore(LocalDateTime.now(ZoneOffset.UTC))) {
            throw new IllegalArgumentException(
                "Deadline is too soon - notification time has already passed. " +
                "Please set a deadline at least 24 hours in advance."
            );
        }
        
        String message = (customMessage != null && !customMessage.isBlank())
            ? customMessage
            : String.format("Reminder: Complete assessment for %s at %s by tomorrow!",
                app != null ? app.getJobTitle() : "position",
                app != null ? app.getCompanyName() : "company");
        
        Notification n = new Notification();
        n.setUserId(userId);
        n.setApplicationId(applicationId);
        n.setType(Notification.NotificationType.DEADLINE);
        n.setMessage(message);
        n.setEventDate(eventDateUtc);
        n.setNotifyAt(notifyAtUtc);
        n.setSent(false);
        n.setRead(false);
        n.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        
        return notificationRepository.save(n);
    }

    /**
     * Create custom notification
     * @param notifyDateTimeStr ISO 8601 datetime string with timezone
     */
    public Notification createCustomNotification(String userId, String applicationId,
                                                String notifyDateTimeStr, String message) {
        // Parse the ISO 8601 string with timezone
        ZonedDateTime notifyDateTime = ZonedDateTime.parse(notifyDateTimeStr);
        
        // Convert to UTC for storage
        ZonedDateTime notifyUtc = notifyDateTime.withZoneSameInstant(ZoneOffset.UTC);
        LocalDateTime notifyAtUtc = notifyUtc.toLocalDateTime();
        
        // Validate notification time hasn't passed
        if (notifyAtUtc.isBefore(LocalDateTime.now(ZoneOffset.UTC))) {
            throw new IllegalArgumentException(
                "Notification time has already passed. Please select a future time."
            );
        }
        
        Notification n = new Notification();
        n.setUserId(userId);
        n.setApplicationId(applicationId);
        n.setType(Notification.NotificationType.CUSTOM);
        n.setMessage(message);
        n.setNotifyAt(notifyAtUtc);
        n.setSent(false);
        n.setRead(false);
        n.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        
        return notificationRepository.save(n);
    }

    /**
     * Get all notifications for a user
     */
    public List<Notification> getUserNotifications(String userId) {
        return notificationRepository.findByUserIdOrderByNotifyAtDesc(userId);
    }

    public List<Notification> getUpcomingNotifications(String userId) {
        LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);
        return notificationRepository.findByUserIdAndNotifyAtAfterOrderByNotifyAtAsc(userId, nowUtc);
    }

    /**
     * Process due notifications - CHECK STATUS BEFORE SENDING FOLLOW-UP
     * ENHANCED: Send emails via AWS SES
     */
    @Scheduled(fixedRate = 60000)
    public void processDueNotifications() {
        LocalDateTime nowUTC = LocalDateTime.now(ZoneOffset.UTC);
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
                        .orElse(new NotificationPreference(user.getUserId(), true, true, LocalDateTime.now(ZoneOffset.UTC)));

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
                
                boolean handled = false;

                // ✅ Apply user’s persistent preferences
                if (pref.isEmailEnabled()) {
                    try {
                        // 🕒 Format time using user's stored timezone
                        ZoneId userZone = getUserZone(n.getUserId());
                        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm a");
                        String localTime = n.getNotifyAt()
                            .atZone(ZoneOffset.UTC)
                            .withZoneSameInstant(userZone)
                            .format(fmt);



                    String html = "<p>" + n.getMessage() + "</p>"
                            + "<p><b>Scheduled for:</b> " + localTime + "</p>";

                    sesService.sendHtmlEmail(user.getEmail(),
                            "📅 Reminder – Upcoming " + n.getType().name().toLowerCase(),
                            html);
                    handled = true;
                    System.out.println("✅ Email sent to " + user.getEmail());
                } catch (Exception e) {
                    System.err.println("❌ Failed to send email: " + e.getMessage());
                    e.printStackTrace();
                    continue;
                }
            }
                
                // ✅ Always keep in-app notifications on
                if (pref.isInAppEnabled()) {
                    saveInAppNotification(user, n, app);
                    handled = true;
                    System.out.println("📱 In-app notification saved for " + user.getEmail());
                }
                    
    
                // Mark as sent
                if (handled) {
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
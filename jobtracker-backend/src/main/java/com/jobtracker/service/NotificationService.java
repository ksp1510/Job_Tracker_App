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
                System.err.println("\u274c Cannot create follow-up reminder: application is null");
                return;
            }
            
            if (app.getId() == null) {
                System.err.println("\u274c Cannot create follow-up reminder: application ID is null");
                return;
            }
            
            if (app.getUserId() == null) {
                System.err.println("\u274c Cannot create follow-up reminder: user ID is null");
                return;
            }
            
            // \u2705 FIXED - Get appliedDate (should be Instant)
            Instant appliedAtInstant = app.getAppliedDate();
            if (appliedAtInstant == null) {
                System.err.println("\u26a0\ufe0f AppliedDate is null, using createdAt");
                appliedAtInstant = app.getCreatedAt();
                if (appliedAtInstant == null) {
                    System.err.println("\u26a0\ufe0f CreatedAt is also null, using current time");
                    appliedAtInstant = Instant.now();
                }
            }
            
            // \u2705 FIXED - Calculate notification time: exactly 7 days after application
            Instant notifyAtInstant = appliedAtInstant.plus(7, java.time.temporal.ChronoUnit.DAYS);
            
            // Create notification
            Notification n = new Notification();
            n.setUserId(app.getUserId());
            n.setApplicationId(app.getId());
            n.setMessage(String.format(
                "Time to follow up on your application at %s for %s",
                app.getCompanyName(),
                app.getJobTitle()
            ));
            n.setNotifyAt(notifyAtInstant);  // \u2705 Store as Instant
            n.setType(Notification.NotificationType.FOLLOW_UP);
            n.setSent(false);
            n.setRead(false);
            n.setCreatedAt(Instant.now());  // \u2705 Store as Instant
            
            // Save notification
            notificationRepository.save(n);
            
            // Log success with timezone-aware display
            ZoneId userZone = getUserZone(app.getUserId());
            System.out.println("\u2705 Follow-up reminder created:");
            System.out.println("   Application ID: " + app.getId());
            System.out.println("   Company: " + app.getCompanyName());
            System.out.println("   Applied on: " + appliedAtInstant);
            System.out.println("   Applied on (User TZ): " + appliedAtInstant.atZone(userZone));
            System.out.println("   Notify at (UTC): " + notifyAtInstant);
            System.out.println("   Notify at (User TZ): " + notifyAtInstant.atZone(userZone));
            
        } catch (Exception e) {
            System.err.println("\u274c Error creating follow-up reminder for application " + 
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
        
        // Convert to Instant for storage
        Instant eventDateInstant = interviewDateTime.toInstant();
        Instant notifyAtInstant = interviewDateTime.minusDays(1).toInstant();
        
        // Validate notification time hasn't passed
        if (notifyAtInstant.isBefore(Instant.now())) {
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
        n.setEventDate(eventDateInstant);  // ✅ Store as Instant
        n.setNotifyAt(notifyAtInstant);    // ✅ Store as Instant
        n.setCreatedAt(Instant.now());     // ✅ Store as Instant
        n.setSent(false);
        n.setRead(false);
        
        return notificationRepository.save(n);
    }

    public Notification createAssessmentDeadlineReminder(String userId, String applicationId,
                                                   String assessmentDeadlineStr, String customMessage) {
        Application app = applicationRepository.findById(applicationId).orElse(null);
        
        // Parse the ISO 8601 string with timezone
        ZonedDateTime deadlineDateTime = ZonedDateTime.parse(assessmentDeadlineStr);
        
        // Convert to Instant for storage
        Instant eventDateInstant = deadlineDateTime.toInstant();
        Instant notifyAtInstant = deadlineDateTime.minusDays(1).toInstant();
        
        // Validate notification time hasn't passed
        if (notifyAtInstant.isBefore(Instant.now())) {
            throw new IllegalArgumentException(
                "Assessment deadline is too soon - notification time has already passed. " +
                "Please set the deadline at least 24 hours in advance."
            );
        }
        
        String message = (customMessage != null && !customMessage.isBlank())
            ? customMessage
            : String.format("Reminder: Assessment deadline tomorrow for %s at %s!",
                app != null ? app.getJobTitle() : "position",
                app != null ? app.getCompanyName() : "company");
        
        Notification n = new Notification();
        n.setUserId(userId);
        n.setApplicationId(applicationId);
        n.setType(Notification.NotificationType.DEADLINE);
        n.setMessage(message);
        n.setEventDate(eventDateInstant);  // ✅ Store as Instant
        n.setNotifyAt(notifyAtInstant);    // ✅ Store as Instant
        n.setCreatedAt(Instant.now());     // ✅ Store as Instant
        n.setSent(false);
        n.setRead(false);
        
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
        Instant notifyAtUtc = notifyUtc.toInstant();
        
        // Validate notification time hasn't passed
        if (notifyAtUtc.isBefore(Instant.now())) {
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
        n.setCreatedAt(Instant.now());
        
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
        // ✅ FIXED - Use Instant.now() instead of LocalDateTime
        Instant nowUTC = Instant.now();
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

                // ✅ FIXED - Use Instant.now() instead of LocalDateTime
                NotificationPreference pref = notificationPreferenceRepository.findByUserId(user.getUserId())
                        .orElse(new NotificationPreference(user.getUserId(), true, true, Instant.now()));

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

                // Apply user's persistent preferences
                if (pref.isEmailEnabled()) {
                    try {
                        // Format time using user's stored timezone
                        sendEmailNotification(user, n, app);
                        handled = true;
                        System.out.println("✅ Email sent to " + user.getEmail());
                    } catch (Exception e) {
                        System.err.println("❌ Failed to send email: " + e.getMessage());
                        e.printStackTrace();
                        continue;
                    }
                }
                
                // Always keep in-app notifications on
                if (pref.isInAppEnabled()) {
                    handled = true;
                    System.out.println("📱 In-app notification enabled for " + user.getEmail());
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
     * Build beautiful HTML email with application details
     */
    private String buildEmailHtml(User user, Notification notification, Application app) {
        // Format time in user's timezone
        ZoneId userZone = getUserZone(notification.getUserId());
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("h:mm a");
        
        String formattedDate = notification.getNotifyAt().atZone(userZone).format(dateFormatter);
        String formattedTime = notification.getNotifyAt().atZone(userZone).format(timeFormatter);
        
        // Get event date if available
        String eventDateStr = "";
        String eventTimeStr = "";
        if (notification.getEventDate() != null) {
            eventDateStr = notification.getEventDate().atZone(userZone).format(dateFormatter);
            eventTimeStr = notification.getEventDate().atZone(userZone).format(timeFormatter);
        }
        
        // Build beautiful HTML
        StringBuilder html = new StringBuilder();
        
        html.append("<!DOCTYPE html>");
        html.append("<html lang='en'>");
        html.append("<head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        html.append("<style>");
        html.append("* { margin: 0; padding: 0; box-sizing: border-box; }");
        html.append("body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; background-color: #f5f7fa; padding: 20px; }");
        html.append(".email-container { max-width: 600px; margin: 0 auto; background: white; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1); }");
        
        // Header styling with gradient based on notification type
        String gradientColor = getGradientColor(notification.getType());
        html.append(".header { background: ").append(gradientColor).append("; color: white; padding: 40px 30px; text-align: center; }");
        html.append(".header-icon { font-size: 48px; margin-bottom: 10px; }");
        html.append(".header h1 { font-size: 28px; font-weight: 600; margin-bottom: 8px; }");
        html.append(".header p { font-size: 16px; opacity: 0.95; }");
        
        // Content styling
        html.append(".content { padding: 40px 30px; }");
        html.append(".greeting { font-size: 18px; color: #1f2937; margin-bottom: 20px; }");
        
        // Alert box styling
        html.append(".alert-box { background: #fef3c7; border-left: 4px solid #f59e0b; padding: 20px; border-radius: 8px; margin: 25px 0; }");
        html.append(".alert-box .alert-title { font-size: 18px; font-weight: 600; color: #92400e; margin-bottom: 8px; }");
        html.append(".alert-box .alert-message { font-size: 16px; color: #78350f; line-height: 1.6; }");
        
        // Application card styling
        html.append(".app-card { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); border-radius: 12px; padding: 25px; margin: 25px 0; color: white; }");
        html.append(".app-card .job-title { font-size: 22px; font-weight: 700; margin-bottom: 8px; }");
        html.append(".app-card .company { font-size: 18px; font-weight: 500; margin-bottom: 8px; opacity: 0.95; }");
        html.append(".app-card .location { font-size: 14px; opacity: 0.9; }");
        
        // Info grid styling
        html.append(".info-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 15px; margin: 25px 0; }");
        html.append(".info-item { background: #f9fafb; border-radius: 8px; padding: 15px; border: 1px solid #e5e7eb; }");
        html.append(".info-item .label { font-size: 12px; color: #6b7280; text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 5px; font-weight: 600; }");
        html.append(".info-item .value { font-size: 16px; color: #1f2937; font-weight: 600; }");
        
        // Button styling
        html.append(".button-container { text-align: center; margin: 30px 0; }");
        html.append(".button { display: inline-block; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; text-decoration: none; padding: 14px 32px; border-radius: 8px; font-size: 16px; font-weight: 600; box-shadow: 0 4px 6px rgba(102, 126, 234, 0.3); transition: all 0.3s; }");
        html.append(".button:hover { box-shadow: 0 6px 8px rgba(102, 126, 234, 0.4); transform: translateY(-2px); }");
        
        // Footer styling
        html.append(".footer { background: #f9fafb; padding: 25px 30px; text-align: center; border-top: 1px solid #e5e7eb; }");
        html.append(".footer p { color: #6b7280; font-size: 14px; line-height: 1.6; margin-bottom: 8px; }");
        html.append(".footer .links { margin-top: 15px; }");
        html.append(".footer a { color: #667eea; text-decoration: none; margin: 0 10px; font-size: 13px; }");
        html.append(".footer a:hover { text-decoration: underline; }");
        
        // Divider
        html.append(".divider { height: 1px; background: linear-gradient(to right, transparent, #e5e7eb, transparent); margin: 25px 0; }");
        
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");
        
        html.append("<div class='email-container'>");
        
        // Header with icon and title
        html.append("<div class='header'>");
        html.append("<div class='header-icon'>").append(getNotificationIcon(notification.getType())).append("</div>");
        html.append("<h1>").append(getNotificationTitle(notification.getType())).append("</h1>");
        html.append("<p>JobTracker Notification</p>");
        html.append("</div>");
        
        // Content
        html.append("<div class='content'>");
        
        // Greeting
        html.append("<p class='greeting'>Hi ").append(user.getFirstName()).append(",</p>");
        
        // Alert box with custom message
        html.append("<div class='alert-box'>");
        html.append("<div class='alert-title'>").append(getAlertTitle(notification.getType())).append("</div>");
        html.append("<div class='alert-message'>").append(notification.getMessage()).append("</div>");
        html.append("</div>");
        
        // Application details card (if available)
        if (app != null) {
            html.append("<div class='app-card'>");
            html.append("<div class='job-title'>📋 ").append(app.getJobTitle()).append("</div>");
            html.append("<div class='company'>🏢 ").append(app.getCompanyName()).append("</div>");
            if (app.getJobLocation() != null && !app.getJobLocation().isEmpty()) {
                html.append("<div class='location'>📍 ").append(app.getJobLocation()).append("</div>");
            }
            html.append("</div>");
        }
        
        // Info grid with event details
        html.append("<div class='info-grid'>");
        
        // Notification type specific info
        if (notification.getEventDate() != null) {
            html.append("<div class='info-item'>");
            html.append("<div class='label'>Event Date</div>");
            html.append("<div class='value'>").append(eventDateStr).append("</div>");
            html.append("</div>");
            
            html.append("<div class='info-item'>");
            html.append("<div class='label'>Event Time</div>");
            html.append("<div class='value'>").append(eventTimeStr).append("</div>");
            html.append("</div>");
        }
        
        html.append("<div class='info-item'>");
        html.append("<div class='label'>Reminder Sent</div>");
        html.append("<div class='value'>").append(formattedDate).append("</div>");
        html.append("</div>");
        
        html.append("<div class='info-item'>");
        html.append("<div class='label'>Time</div>");
        html.append("<div class='value'>").append(formattedTime).append("</div>");
        html.append("</div>");
        
        html.append("</div>"); // Close info-grid
        
        // Action button
        html.append("<div class='button-container'>");
        html.append("<a href='http://localhost:3000/applications' class='button'>View Application Details</a>");
        html.append("</div>");
        
        // Divider
        html.append("<div class='divider'></div>");
        
        // Tips section based on notification type
        html.append(getTipsSection(notification.getType()));
        
        html.append("</div>"); // Close content
        
        // Footer
        html.append("<div class='footer'>");
        html.append("<p><strong>JobTracker</strong> - Your Personal Job Application Assistant</p>");
        html.append("<p>This is an automated reminder to help you stay on top of your job search.</p>");
        html.append("<div class='links'>");
        html.append("<a href='http://localhost:3000/settings'>Manage Notifications</a>");
        html.append("<a href='http://localhost:3000/applications'>View All Applications</a>");
        html.append("<a href='http://localhost:3000/help'>Get Help</a>");
        html.append("</div>");
        html.append("</div>");
        
        html.append("</div>"); // Close email-container
        html.append("</body>");
        html.append("</html>");
        
        return html.toString();
    }

    /**
     * Get gradient color based on notification type
     */
    private String getGradientColor(Notification.NotificationType type) {
        switch (type) {
            case INTERVIEW:
                return "linear-gradient(135deg, #667eea 0%, #764ba2 100%)";
            case DEADLINE:
                return "linear-gradient(135deg, #f093fb 0%, #f5576c 100%)";
            case FOLLOW_UP:
                return "linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)";
            case CUSTOM:
                return "linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)";
            default:
                return "linear-gradient(135deg, #667eea 0%, #764ba2 100%)";
        }
    }

    /**
     * Get emoji icon based on notification type
     */
    private String getNotificationIcon(Notification.NotificationType type) {
        switch (type) {
            case INTERVIEW:
                return "🎯";
            case DEADLINE:
                return "⏰";
            case FOLLOW_UP:
                return "📬";
            case CUSTOM:
                return "🔔";
            default:
                return "📋";
        }
    }

    /**
     * Get notification title based on type
     */
    private String getNotificationTitle(Notification.NotificationType type) {
        switch (type) {
            case INTERVIEW:
                return "Interview Reminder";
            case DEADLINE:
                return "Deadline Alert";
            case FOLLOW_UP:
                return "Follow-Up Reminder";
            case CUSTOM:
                return "Custom Reminder";
            default:
                return "Job Application Reminder";
        }
    }

    /**
     * Get alert title based on notification type
     */
    private String getAlertTitle(Notification.NotificationType type) {
        switch (type) {
            case INTERVIEW:
                return "⚡ Your Interview is Tomorrow!";
            case DEADLINE:
                return "⚠️ Deadline Approaching!";
            case FOLLOW_UP:
                return "💼 Time to Follow Up";
            case CUSTOM:
                return "📌 Reminder";
            default:
                return "📢 Important Reminder";
        }
    }

    /**
     * Get helpful tips based on notification type
     */
    private String getTipsSection(Notification.NotificationType type) {
        StringBuilder tips = new StringBuilder();
        
        tips.append("<div style='background: #f0f9ff; border-radius: 8px; padding: 20px; border-left: 4px solid #0284c7;'>");
        tips.append("<h3 style='color: #0c4a6e; font-size: 16px; margin-bottom: 12px; font-weight: 600;'>💡 Quick Tips</h3>");
        tips.append("<ul style='color: #075985; font-size: 14px; line-height: 1.8; padding-left: 20px;'>");
        
        switch (type) {
            case INTERVIEW:
                tips.append("<li>Review the job description and your resume</li>");
                tips.append("<li>Prepare answers for common interview questions</li>");
                tips.append("<li>Research the company and interviewer on LinkedIn</li>");
                tips.append("<li>Test your video/audio setup if it's a virtual interview</li>");
                tips.append("<li>Prepare thoughtful questions to ask the interviewer</li>");
                break;
            case DEADLINE:
                tips.append("<li>Set aside dedicated time to complete the assessment</li>");
                tips.append("<li>Review any materials or instructions provided</li>");
                tips.append("<li>Test your internet connection and equipment</li>");
                tips.append("<li>Complete in a quiet environment without distractions</li>");
                tips.append("<li>Submit with time to spare in case of technical issues</li>");
                break;
            case FOLLOW_UP:
                tips.append("<li>Send a polite follow-up email to the hiring manager</li>");
                tips.append("<li>Reference your application date and position</li>");
                tips.append("<li>Express continued interest in the role</li>");
                tips.append("<li>Keep it brief and professional (3-4 sentences)</li>");
                tips.append("<li>Include any updates to your qualifications</li>");
                break;
            default:
                tips.append("<li>Stay organized with your job search</li>");
                tips.append("<li>Keep track of all your applications</li>");
                tips.append("<li>Follow up regularly with recruiters</li>");
                tips.append("<li>Continue improving your skills</li>");
                break;
        }
        
        tips.append("</ul>");
        tips.append("</div>");
        
        return tips.toString();
    }

    private String getEmailSubject(Notification.NotificationType type) {
        switch (type) {
            case FOLLOW_UP:
                return "🔔 Follow-up Reminder - Job Application";
            case INTERVIEW:
                return "📅 Interview Reminder - Tomorrow!";
            case DEADLINE:
                return "⏰ Assessment Deadline - Tomorrow!";
            case CUSTOM:
                return "🔔 Custom Notification";
            default:
                return "🔔 Job Tracker Notification";
        }
    }
}
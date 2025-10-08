package com.jobtracker.service;

import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

@Service
public class SesService {

    private final SesClient sesClient;
    
    // For development/testing - in production, this should come from application.properties
    private final String senderEmail = "ksp1510@gmail.com"; // Your verified SES email

    public SesService(SesClient sesClient) {
        this.sesClient = sesClient;
    }

    /**
     * Send email using AWS SES
     * @param toAddress Recipient email address
     * @param subject Email subject
     * @param body Email body (plain text)
     */
    public void sendEmail(String toAddress, String subject, String body) {
        try {
            // For development, use hardcoded email. In production, use toAddress
            String recipientEmail = "ksp1510@gmail.com"; // For testing
            // String recipientEmail = toAddress; // Use this in production
            
            Destination destination = Destination.builder()
                    .toAddresses(recipientEmail)
                    .build();

            Message msg = Message.builder()
                    .subject(Content.builder().data(subject).build())
                    .body(Body.builder()
                            .text(Content.builder().data(body).build())
                            .build())
                    .build();

            SendEmailRequest req = SendEmailRequest.builder()
                    .destination(destination)
                    .message(msg)
                    .source(senderEmail)
                    .build();

            SendEmailResponse response = sesClient.sendEmail(req);
            System.out.println("📧 HTML Email sent successfully to " + recipientEmail + 
                             " (MessageId: " + response.messageId() + ")");
            
        } catch (Exception e) {
            System.err.println("❌ Failed to send HTML email: " + e.getMessage());
            throw new RuntimeException("Failed to send HTML email: " + e.getMessage(), e);
        }
    }

    /**
     * Send HTML email
     */
    public void sendHtmlEmail(String toAddress, String subject, String htmlBody) {
        try {
            // FIXED: Use actual recipient email instead of hardcoded test email
            Destination destination = Destination.builder()
                    .toAddresses(toAddress)  // Use actual recipient
                    .build();
    
            Message msg = Message.builder()
                    .subject(Content.builder().data(subject).build())
                    .body(Body.builder()
                            .html(Content.builder().data(htmlBody).build())
                            .build())
                    .build();
    
            SendEmailRequest req = SendEmailRequest.builder()
                    .destination(destination)
                    .message(msg)
                    .source(senderEmail)
                    .build();
    
            SendEmailResponse response = sesClient.sendEmail(req);
            System.out.println("📧 Email sent successfully to " + toAddress + 
                             " (MessageId: " + response.messageId() + ")");
            
        } catch (Exception e) {
            System.err.println("❌ Failed to send email: " + e.getMessage());
            throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
        }
    }
}

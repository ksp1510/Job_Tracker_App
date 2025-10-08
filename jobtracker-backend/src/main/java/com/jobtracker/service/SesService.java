package com.jobtracker.service;

import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

@Service
public class SesService {

    private final SesClient sesClient;
    private final String senderEmail = "patelkishan101097@gmail.com";

    public SesService(SesClient sesClient) {
        this.sesClient = sesClient;
    }

    public void sendHtmlEmail(String toAddress, String subject, String htmlBody) {
        System.out.println("📧 ========== EMAIL SENDING DEBUG ==========");
        System.out.println("📧 Time: " + java.time.LocalDateTime.now());
        System.out.println("📧 Sender: " + senderEmail);
        System.out.println("📧 Recipient: " + toAddress);
        System.out.println("📧 Subject: " + subject);
        
        try {
            Destination destination = Destination.builder()
                    .toAddresses(toAddress)
                    .build();
            System.out.println("✅ Destination built");

            Message msg = Message.builder()
                    .subject(Content.builder().data(subject).build())
                    .body(Body.builder()
                            .html(Content.builder().data(htmlBody).build())
                            .build())
                    .build();
            System.out.println("✅ Message built");

            SendEmailRequest req = SendEmailRequest.builder()
                    .destination(destination)
                    .message(msg)
                    .source(senderEmail)
                    .build();
            System.out.println("✅ Request built");

            System.out.println("📤 Sending via AWS SES...");
            SendEmailResponse response = sesClient.sendEmail(req);
            
            System.out.println("✅ ========== EMAIL SENT SUCCESSFULLY ==========");
            System.out.println("✅ MessageId: " + response.messageId());
            System.out.println("✅ To: " + toAddress);
            System.out.println("================================================");
            
        } catch (MessageRejectedException e) {
            System.err.println("❌ EMAIL REJECTED");
            System.err.println("❌ Message: " + e.getMessage());
            System.err.println("❌ Reason: " + e.awsErrorDetails().errorMessage());
            System.err.println("❌ Action: Verify email in AWS SES Console");
            throw new RuntimeException("Email rejected: " + e.getMessage(), e);
            
        } catch (SesException e) {
            System.err.println("❌ AWS SES ERROR");
            System.err.println("❌ Error Code: " + e.awsErrorDetails().errorCode());
            System.err.println("❌ Error Message: " + e.awsErrorDetails().errorMessage());
            System.err.println("❌ Service: " + e.awsErrorDetails().serviceName());
            throw new RuntimeException("SES error: " + e.getMessage(), e);
            
        } catch (Exception e) {
            System.err.println("❌ UNEXPECTED ERROR");
            System.err.println("❌ Type: " + e.getClass().getName());
            System.err.println("❌ Message: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
        }
    }
}
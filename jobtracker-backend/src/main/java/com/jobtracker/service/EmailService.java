package com.jobtracker.service;

import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

@Service
public class EmailService {

    private final SesClient sesClient;

    public EmailService() {
        this.sesClient = SesClient.builder()
                .region(Region.US_EAST_1) // Change to your SES region
                .build();
    }

    public void sendEmail(String to, String subject, String body) {
        Destination destination = Destination.builder()
                .toAddresses(to)
                .build();

        Content subjContent = Content.builder()
                .data(subject)
                .build();

        Content textContent = Content.builder()
                .data(body)
                .build();

        Body msgBody = Body.builder()
                .text(textContent)
                .build();

        Message message = Message.builder()
                .subject(subjContent)
                .body(msgBody)
                .build();

        SendEmailRequest request = SendEmailRequest.builder()
                .destination(destination)
                .message(message)
                .source("ksp1510@gmail.com") // Must be verified in SES
                .build();

        sesClient.sendEmail(request);
    }
}

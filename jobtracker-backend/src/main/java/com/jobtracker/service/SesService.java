package com.jobtracker.service;

import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

@Service
public class SesService {

    private final SesClient sesClient;

    public SesService(SesClient sesClient) {
        this.sesClient = sesClient;
    }

    public void sendEmail(String toAddress, String subject, String body) {
        Destination destination = Destination.builder()
                .toAddresses(toAddress)
                .build();

        Message msg = Message.builder()
                .subject(Content.builder().data(subject).build())
                .body(Body.builder().text(Content.builder().data(body).build()).build())
                .build();

        SendEmailRequest req = SendEmailRequest.builder()
                .destination(destination)
                .message(msg)
                .source("noreply@yourdomain.com")
                .build();

        sesClient.sendEmail(req);
    }
}

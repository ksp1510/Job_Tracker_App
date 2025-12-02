package com.jobtracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {
    private String message;
    private boolean success = true;

    public MessageResponse(String message) {
        this.message = message;
        this.success = true;
    }
}
package com.jobtracker.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class FileDownloadResponse {

    private byte[] content;
    private String fileName;
    private String contentType;
}

package com.africa.hr.dto;

import lombok.Data;

@Data
public class DocumentDTO {
    private String name;
    private String blobUrl;
    // Optionally for response:
    private Boolean visible;
    private String uploadedAt;
}
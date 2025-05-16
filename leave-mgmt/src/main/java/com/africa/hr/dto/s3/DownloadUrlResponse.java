package com.africa.hr.dto.s3;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DownloadUrlResponse {
    private String downloadUrl;
    private String fileName;
    private String contentType;
    private LocalDateTime expiresAt;
} 
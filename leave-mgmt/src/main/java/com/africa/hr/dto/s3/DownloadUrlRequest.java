package com.africa.hr.dto.s3;

import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DownloadUrlRequest {

    private String fileName;

    @Positive(message = "URL expiration must be positive")
    private Integer expiresIn = 3600; // Default 1 hour

    private Boolean inline = false; // Default to download (not inline viewing)
}
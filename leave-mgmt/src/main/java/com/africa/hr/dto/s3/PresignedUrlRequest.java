package com.africa.hr.dto.s3;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PresignedUrlRequest {

    @NotBlank(message = "File name is required")
    private String fileName;

    private String contentType;

    @Positive(message = "URL expiration must be positive")
    private Integer expiresIn = 3600; // Default 1 hour

    private String folder = "leave-documents"; // Default folder
}
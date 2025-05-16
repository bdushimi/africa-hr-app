package com.africa.hr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresignedUrlResponseDto {
    private String url;
    private String fileKey;
    private String downloadUrl;
    private Integer expiresIn;
    private String error;
}
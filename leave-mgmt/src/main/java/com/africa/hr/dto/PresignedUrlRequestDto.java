package com.africa.hr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class PresignedUrlRequestDto {
    private String fileName;
    private String contentType;
    private String folder;
    private Integer expiresIn;
    private Long contentLength;
}
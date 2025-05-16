package com.africa.hr.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PresignUploadRequestDto {
    @NotBlank
    private String filename;
    @NotBlank
    private String mimeType;
}
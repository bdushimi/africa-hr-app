package com.africa.hr.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddDocumentRequestDto {
    @NotBlank
    private String name;
    @NotBlank
    private String blobUrl;
}
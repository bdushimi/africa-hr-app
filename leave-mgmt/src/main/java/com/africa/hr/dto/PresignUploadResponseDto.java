package com.africa.hr.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PresignUploadResponseDto {
    private String uploadUrl;
    private String blobUrl;
}
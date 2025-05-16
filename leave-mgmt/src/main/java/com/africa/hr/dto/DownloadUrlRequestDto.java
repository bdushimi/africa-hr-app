package com.africa.hr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DownloadUrlRequestDto {
    private Integer expiresIn;
    private String fileName;
    private Boolean inline;
}
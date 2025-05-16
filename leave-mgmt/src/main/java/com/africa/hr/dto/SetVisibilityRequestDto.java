package com.africa.hr.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SetVisibilityRequestDto {
    @NotNull
    private Boolean visible;
}
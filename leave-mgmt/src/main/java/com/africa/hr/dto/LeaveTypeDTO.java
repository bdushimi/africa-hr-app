package com.africa.hr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LeaveTypeDTO {
    private Long id;

    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    @NotNull(message = "Default flag is required")
    private Boolean isDefault;

    @NotNull(message = "Enabled flag is required")
    private Boolean isEnabled = true;

    private Integer maxDuration;

    @NotNull(message = "Paid flag is required")
    private Boolean paid;

    private java.math.BigDecimal accrualRate;

    @NotNull(message = "Accrual based flag is required")
    private Boolean accrualBased;

    private Boolean isCarryForwardEnabled;

    private java.math.BigDecimal carryForwardCap;

    @NotNull(message = "Require reason flag is required")
    private Boolean requireReason = false;

    @NotNull(message = "Require document flag is required")
    private Boolean requireDocument = false;
}
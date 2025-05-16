package com.africa.hr.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class LeaveTypeUpdateDTO {
    private String name;
    private String description;
    private Boolean isDefault;
    private Boolean isEnabled;
    private Integer maxDuration;
    private Boolean paid;
    private Boolean accrualBased;

    @DecimalMin(value = "0.0", message = "Accrual rate must be greater than or equal to 0")
    private BigDecimal accrualRate;

    @NotNull(message = "Carry-forward enabled flag is required")
    private Boolean isCarryForwardEnabled;

    @DecimalMin(value = "0.0", message = "Carry-forward cap must be greater than or equal to 0")
    private BigDecimal carryForwardCap;

    private Boolean requireReason;

    private Boolean requireDocument;
}
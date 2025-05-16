package com.africa.hr.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

@Data
public class LeaveAccrualResponseDTO {
    private Long id;
    private Long employeeBalanceId;
    private Long employeeId;
    private String employeeName;
    private Long leaveTypeId;
    private String leaveTypeName;
    private YearMonth yearMonth;
    private BigDecimal amount;
    private Boolean isProrated;
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate accrualDate;

    @JsonFormat(pattern = "yyyy-MM")
    private YearMonth accrualPeriod;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime processedDate;
}
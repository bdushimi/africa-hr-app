package com.africa.hr.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.math.BigDecimal;

@Data
public class LeaveAccrualSummaryDTO {
    private Long id;
    private String accrualPeriod;
    private LocalDateTime processedDate;
    private Integer employeeCount;
    private Integer totalAccruals;
    private BigDecimal totalDaysAccrued;
    private String status; // COMPLETED, PARTIAL, FAILED
}
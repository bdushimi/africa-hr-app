package com.africa.hr.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaveCarryForwardResponseDTO {
    private Long id;
    private Long employeeBalanceId;
    private Long employeeId;
    private String employeeName;
    private Long leaveTypeId;
    private String leaveTypeName;
    private Integer fromYear;
    private Integer toYear;
    private BigDecimal originalBalance;
    private BigDecimal currentBalance;
    private BigDecimal carryForwardCap;
    private BigDecimal amountToBeCarriedForward;
    private BigDecimal amountToBeForfeited;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
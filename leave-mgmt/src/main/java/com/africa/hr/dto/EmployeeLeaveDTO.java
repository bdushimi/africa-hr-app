package com.africa.hr.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for employee leave information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeLeaveDTO {
    private String employeeId;
    private String employeeName;
    private String departmentName;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    private String leaveType;
    private String status;
    private Double days;

    // Approver information
    private String approverId;
    private String approverName;
}
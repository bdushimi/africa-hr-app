package com.africa.hr.dto;

import com.africa.hr.model.LeaveRequestStatus;
import com.africa.hr.validation.annotations.ValidLeaveRequestDates;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
@ValidLeaveRequestDates
public class LeaveRequestDTO {
    private Long id;

    @NotNull(message = "Leave type ID is required")
    private Long leaveTypeId;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    private Boolean halfDayStart = false;
    private Boolean halfDayEnd = false;

    private String leaveRequestReason;

    private java.util.List<DocumentDTO> documents;

    private DocumentDTO primaryDocument;

    // Response fields
    private LeaveRequestStatus status;
    private String rejectionReason;
    private String employeeName;
    private String leaveTypeName;
    private String department;
    private Boolean requireReason;
    private Boolean requireDocument;
    private String approvedByName;
    private String createdAt;
    private String updatedAt;

    // No-args constructor for test and framework compatibility
    public LeaveRequestDTO() {
    }

    // Add a constructor for mapping
    public LeaveRequestDTO(Long id, String employeeName, String department, String leaveType, LocalDate startDate,
            LocalDate endDate, LeaveRequestStatus status) {
        this.id = id;
        this.employeeName = employeeName;
        this.department = department;
        this.leaveTypeName = leaveType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
    }
}
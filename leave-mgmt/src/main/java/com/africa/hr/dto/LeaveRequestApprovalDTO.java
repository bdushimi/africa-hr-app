package com.africa.hr.dto;

import com.africa.hr.model.LeaveRequestStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LeaveRequestApprovalDTO {
    @NotNull(message = "Status is required")
    private LeaveRequestStatus status;

    private String rejectionReason;
}
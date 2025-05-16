package com.africa.hr.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class LeaveRequestListResponseDTO {
    private List<LeaveRequestItemDTO> leaveRequests;
    private PaginationDTO pagination;

    @Data
    public static class LeaveRequestItemDTO {
        private String id;
        private String type;

        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate startDate;

        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate endDate;

        private Double days;
        private String status;
        private String employeeId;
        private String employeeName;
        private String reason;
        private Boolean requireReason;
        private Boolean requireDocument;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime createdAt;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime updatedAt;

        private String managerId;
        private String managerName;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime approvedAt;

        private String comments;

        private java.util.List<DocumentDTO> documents;
        private DocumentDTO primaryDocument;
    }

    @Data
    public static class PaginationDTO {
        private long total;
        private int page;
        private int pageSize;
        private int totalPages;
    }
}
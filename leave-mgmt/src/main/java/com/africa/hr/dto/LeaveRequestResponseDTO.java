package com.africa.hr.dto;

import com.africa.hr.model.LeaveRequest;
import com.africa.hr.model.LeaveRequestStatus;
import com.africa.hr.model.Document;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Data
public class LeaveRequestResponseDTO {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private Long leaveTypeId;
    private String leaveTypeName;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean halfDayStart;
    private Boolean halfDayEnd;
    private String leaveRequestReason;
    private String documentUrl;
    private String documentKey;
    private LeaveRequestStatus status;
    private String rejectionReason;
    private Long managerId;
    private String managerName;
    private LocalDateTime approvedAt;
    private LocalDateTime createdAt;
    private Boolean requireReason;
    private Boolean requireDocument;
    private String error;
    private DepartmentDTO department;
    private String departmentName;
    private List<DocumentDTO> documents;
    private DocumentDTO primaryDocument;

    public static LeaveRequestResponseDTO fromEntity(LeaveRequest leaveRequest) {
        LeaveRequestResponseDTO dto = new LeaveRequestResponseDTO();
        dto.setId(leaveRequest.getId());
        dto.setEmployeeId(leaveRequest.getEmployee().getId());
        dto.setEmployeeName(leaveRequest.getEmployee().getFullName());
        dto.setLeaveTypeId(leaveRequest.getLeaveType().getId());
        dto.setLeaveTypeName(leaveRequest.getLeaveType().getName());
        dto.setStartDate(leaveRequest.getStartDate());
        dto.setEndDate(leaveRequest.getEndDate());
        dto.setHalfDayStart(leaveRequest.getHalfDayStart());
        dto.setHalfDayEnd(leaveRequest.getHalfDayEnd());
        dto.setLeaveRequestReason(leaveRequest.getLeaveRequestReason());
        dto.setStatus(leaveRequest.getStatus());
        dto.setRejectionReason(leaveRequest.getRejectionReason());
        dto.setApprovedAt(leaveRequest.getApprovedAt());
        dto.setCreatedAt(leaveRequest.getCreatedAt());
        dto.setRequireReason(leaveRequest.getLeaveType().getRequireReason());
        dto.setRequireDocument(leaveRequest.getLeaveType().getRequireDocument());

        if (leaveRequest.getManager() != null) {
            dto.setManagerId(leaveRequest.getManager().getId());
            dto.setManagerName(leaveRequest.getManager().getFullName());
        }

        if (leaveRequest.getEmployee().getDepartment() != null) {
            dto.setDepartmentName(leaveRequest.getEmployee().getDepartment().getName());
        }

        // Map documents
        if (leaveRequest.getDocuments() != null) {
            dto.setDocuments(leaveRequest.getDocuments().stream()
                    .map(doc -> {
                        DocumentDTO docDto = new DocumentDTO();
                        docDto.setName(doc.getName());
                        docDto.setBlobUrl(doc.getBlobUrl());
                        docDto.setVisible(doc.isVisible());
                        docDto.setUploadedAt(doc.getUploadedAt() != null
                                ? doc.getUploadedAt().format(DateTimeFormatter.ISO_DATE_TIME)
                                : null);
                        return docDto;
                    })
                    .collect(java.util.stream.Collectors.toList()));
        }

        // Map primary document
        if (leaveRequest.getPrimaryDocument() != null) {
            Document doc = leaveRequest.getPrimaryDocument();
            DocumentDTO docDto = new DocumentDTO();
            docDto.setName(doc.getName());
            docDto.setBlobUrl(doc.getBlobUrl());
            docDto.setVisible(doc.isVisible());
            docDto.setUploadedAt(
                    doc.getUploadedAt() != null ? doc.getUploadedAt().format(DateTimeFormatter.ISO_DATE_TIME) : null);
            dto.setPrimaryDocument(docDto);
        }

        return dto;
    }

    public static LeaveRequestResponseDTO error(String errorMessage) {
        LeaveRequestResponseDTO dto = new LeaveRequestResponseDTO();
        dto.setError(errorMessage);
        return dto;
    }

    public static LeaveRequestResponseDTO fromDTO(LeaveRequestDTO dto) {
        LeaveRequestResponseDTO response = new LeaveRequestResponseDTO();
        response.setId(dto.getId());
        response.setEmployeeName(dto.getEmployeeName());
        response.setLeaveTypeName(dto.getLeaveTypeName());
        response.setStartDate(dto.getStartDate());
        response.setEndDate(dto.getEndDate());
        response.setStatus(dto.getStatus());
        // Set department name directly if available
        if (dto.getDepartment() != null) {
            response.setDepartmentName(dto.getDepartment());
        }
        // Map other fields as needed
        return response;
    }
}
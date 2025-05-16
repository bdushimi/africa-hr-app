package com.africa.hr.controller;

import com.africa.hr.dto.LeaveRequestDTO;
import com.africa.hr.dto.LeaveRequestApprovalDTO;
import com.africa.hr.dto.LeaveRequestResponseDTO;
import com.africa.hr.dto.LeaveRequestListResponseDTO;
import com.africa.hr.dto.CompanyCalendarDTO;
import com.africa.hr.dto.PublicHolidayDTO;
import com.africa.hr.model.LeaveRequest;
import com.africa.hr.model.LeaveRequestStatus;
import com.africa.hr.model.User;
import com.africa.hr.model.PublicHoliday;
import com.africa.hr.model.Document;
import com.africa.hr.dto.DocumentDTO;
import com.africa.hr.service.LeaveRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.time.temporal.ChronoUnit;
import java.time.LocalDate;

@RestController
@RequestMapping("/leaveRequests")
@RequiredArgsConstructor
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;

    @PostMapping
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    public ResponseEntity<LeaveRequestResponseDTO> submitLeaveRequest(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody LeaveRequestDTO leaveRequestDTO) {
        try {
            LeaveRequest leaveRequest = leaveRequestService.submitLeaveRequest(user, leaveRequestDTO);
            return ResponseEntity.ok(LeaveRequestResponseDTO.fromEntity(leaveRequest));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(LeaveRequestResponseDTO.error(e.getMessage()));
        }
    }

    @PutMapping("/{requestId}/approve")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<LeaveRequestResponseDTO> approveLeaveRequest(
            @PathVariable Long requestId,
            @AuthenticationPrincipal User approver,
            @Valid @RequestBody LeaveRequestApprovalDTO approvalDTO) {
        LeaveRequest leaveRequest = leaveRequestService.approveLeaveRequest(requestId, approver, approvalDTO);
        return ResponseEntity.ok(LeaveRequestResponseDTO.fromEntity(leaveRequest));
    }

    @GetMapping("/department/{departmentId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Page<LeaveRequestResponseDTO>> getLeaveRequestsByDepartment(
            @PathVariable Long departmentId,
            @RequestParam(required = false) LeaveRequestStatus status,
            Pageable pageable) {
        Page<LeaveRequest> leaveRequests = leaveRequestService.getLeaveRequestsByDepartment(departmentId, status,
                pageable);
        Page<LeaveRequestResponseDTO> responseDTOs = leaveRequests.map(LeaveRequestResponseDTO::fromEntity);
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/company-calendar")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'STAFF')")
    public ResponseEntity<CompanyCalendarDTO> getCompanyCalendar(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {

        // Use current year if not provided
        LocalDate now = LocalDate.now();
        int targetYear = year != null ? year : now.getYear();

        // Month is optional - if not provided, return data for the entire year
        Integer targetMonth = month;

        // Get all employee leave records and public holidays for the specified year and
        // month/entire year
        CompanyCalendarDTO response = leaveRequestService.getCompanyCalendarData(targetYear, targetMonth);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER')")
    public ResponseEntity<LeaveRequestListResponseDTO> getMyLeaveRequests(
            @AuthenticationPrincipal User employee,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<LeaveRequest> leaveRequestsPage = leaveRequestService.getLeaveRequestsByEmployee(employee, pageable);

        LeaveRequestListResponseDTO response = new LeaveRequestListResponseDTO();

        // Convert leave requests to DTOs
        List<LeaveRequestListResponseDTO.LeaveRequestItemDTO> leaveRequestDTOs = leaveRequestsPage.getContent().stream()
                .map(request -> {
                    LeaveRequestListResponseDTO.LeaveRequestItemDTO dto = new LeaveRequestListResponseDTO.LeaveRequestItemDTO();
                    dto.setId(request.getId().toString());
                    dto.setType(request.getLeaveType().getName());
                    dto.setStartDate(request.getStartDate());
                    dto.setEndDate(request.getEndDate());

                    // Calculate days excluding weekends
                    LocalDate startDate = request.getStartDate();
                    LocalDate endDate = request.getEndDate();
                    double days = 0;

                    for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                        // Skip weekends (Saturday = 6, Sunday = 7 in DayOfWeek enum)
                        if (date.getDayOfWeek().getValue() < 6) {
                            days += 1.0;
                        }
                    }

                    // Adjust for half days
                    if (Boolean.TRUE.equals(request.getHalfDayStart()) && startDate.getDayOfWeek().getValue() < 6) {
                        days -= 0.5;
                    }
                    if (Boolean.TRUE.equals(request.getHalfDayEnd()) && endDate.getDayOfWeek().getValue() < 6) {
                        days -= 0.5;
                    }

                    dto.setDays(days);

                    dto.setStatus(request.getStatus().name());
                    dto.setEmployeeId(request.getEmployee().getId().toString());
                    dto.setEmployeeName(request.getEmployee().getFullName());
                    dto.setReason(request.getLeaveRequestReason());
                    dto.setRequireReason(request.getLeaveType().getRequireReason());
                    dto.setRequireDocument(request.getLeaveType().getRequireDocument());
                    dto.setCreatedAt(request.getCreatedAt());
                    dto.setUpdatedAt(request.getUpdatedAt());

                    if (request.getManager() != null) {
                        dto.setManagerId(request.getManager().getId().toString());
                        dto.setManagerName(request.getManager().getFullName());
                    }
                    dto.setApprovedAt(request.getApprovedAt());
                    dto.setComments(request.getRejectionReason());

                    // Map documents
                    if (request.getDocuments() != null) {
                        dto.setDocuments(request.getDocuments().stream().map(doc -> {
                            DocumentDTO d = new DocumentDTO();
                            d.setName(doc.getName());
                            d.setBlobUrl(doc.getBlobUrl());
                            d.setVisible(doc.isVisible());
                            d.setUploadedAt(doc.getUploadedAt() != null ? doc.getUploadedAt().toString() : null);
                            return d;
                        }).collect(Collectors.toList()));
                    }
                    // Map primary document
                    if (request.getPrimaryDocument() != null) {
                        Document doc = request.getPrimaryDocument();
                        DocumentDTO d = new DocumentDTO();
                        d.setName(doc.getName());
                        d.setBlobUrl(doc.getBlobUrl());
                        d.setVisible(doc.isVisible());
                        d.setUploadedAt(doc.getUploadedAt() != null ? doc.getUploadedAt().toString() : null);
                        dto.setPrimaryDocument(d);
                    }

                    return dto;
                })
                .collect(Collectors.toList());

        response.setLeaveRequests(leaveRequestDTOs);

        // Set pagination info
        LeaveRequestListResponseDTO.PaginationDTO pagination = new LeaveRequestListResponseDTO.PaginationDTO();
        pagination.setTotal(leaveRequestsPage.getTotalElements());
        pagination.setPage(page);
        pagination.setPageSize(size);
        pagination.setTotalPages(leaveRequestsPage.getTotalPages());
        response.setPagination(pagination);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER')")
    public ResponseEntity<Map<String, Long>> getMyLeaveRequestsSummary(
            @AuthenticationPrincipal User employee) {
        Map<String, Long> summary = new HashMap<>();

        // Get counts for each status
        summary.put("PENDING",
                (long) leaveRequestService.getLeaveRequestsByEmployeeAndStatus(employee, LeaveRequestStatus.PENDING)
                        .size());
        summary.put("APPROVED",
                (long) leaveRequestService.getLeaveRequestsByEmployeeAndStatus(employee, LeaveRequestStatus.APPROVED)
                        .size());
        summary.put("REJECTED",
                (long) leaveRequestService.getLeaveRequestsByEmployeeAndStatus(employee, LeaveRequestStatus.REJECTED)
                        .size());
        summary.put("CANCELLED",
                (long) leaveRequestService.getLeaveRequestsByEmployeeAndStatus(employee, LeaveRequestStatus.CANCELLED)
                        .size());

        return ResponseEntity.ok(summary);
    }

    @GetMapping("/{requestId}")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER')")
    public ResponseEntity<LeaveRequestResponseDTO> getLeaveRequest(@PathVariable Long requestId) {
        LeaveRequest leaveRequest = leaveRequestService.getLeaveRequest(requestId);
        return ResponseEntity.ok(LeaveRequestResponseDTO.fromEntity(leaveRequest));
    }

    @PatchMapping("/{requestId}/cancel")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER')")
    public ResponseEntity<LeaveRequestResponseDTO> cancelLeaveRequest(
            @PathVariable Long requestId,
            @AuthenticationPrincipal User employee) {
        LeaveRequestResponseDTO responseDTO = leaveRequestService.cancelLeaveRequest(requestId, employee);
        return ResponseEntity.ok(responseDTO);
    }

    @GetMapping("/direct-reports")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Page<LeaveRequestResponseDTO>> getDirectReportsLeaveRequests(
            @AuthenticationPrincipal User manager,
            @RequestParam(required = false) LeaveRequestStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {

        Pageable pageable = PageRequest.of(page, size);
        // Get leave requests where manager_id matches the authenticated user's ID
        Page<LeaveRequest> leaveRequests = leaveRequestService.getLeaveRequestsByManagerId(manager.getId(), status,
                pageable);

        Page<LeaveRequestResponseDTO> responseDTOs = leaveRequests.map(LeaveRequestResponseDTO::fromEntity);

        return ResponseEntity.ok(responseDTOs);
    }

}
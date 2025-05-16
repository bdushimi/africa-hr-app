package com.africa.hr.service;

import com.africa.hr.dto.LeaveRequestDTO;
import com.africa.hr.dto.LeaveRequestApprovalDTO;
import com.africa.hr.dto.CompanyCalendarDTO;
import com.africa.hr.dto.EmployeeLeaveDTO;
import com.africa.hr.dto.PublicHolidayDTO;
import com.africa.hr.dto.DepartmentDTO;
import com.africa.hr.dto.DocumentDTO;
import com.africa.hr.dto.LeaveRequestResponseDTO;
import com.africa.hr.model.*;
import com.africa.hr.repository.LeaveRequestRepository;
import com.africa.hr.repository.DocumentRepository;
import com.africa.hr.service.email.EmailService;
import com.africa.hr.websocket.WebSocketNotificationService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveRequestService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final DocumentRepository documentRepository;
    private final LeaveTypeService leaveTypeService;
    private final EmployeeBalanceService employeeBalanceService;
    private final EmailService emailService;
    private final PublicHolidayService publicHolidayService;
    private final DepartmentService departmentService;
    private final WebSocketNotificationService notificationService;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public LeaveRequest submitLeaveRequest(User employee, LeaveRequestDTO requestDTO) {
        log.info("Submitting leave request for employee: {}", employee.getId());

        // Load employee with manager relationship
        employee = leaveRequestRepository.findEmployeeWithManager(employee.getId())
                .orElseThrow(() -> new EntityNotFoundException("Employee not found"));

        // Validate leave type
        LeaveType leaveType = leaveTypeService.getLeaveType(requestDTO.getLeaveTypeId());

        // Check if leave type is enabled
        if (!leaveType.getIsEnabled()) {
            throw new IllegalStateException("Leave type '" + leaveType.getName() + "' is currently disabled");
        }

        // Validate reason requirement
        if (Boolean.TRUE.equals(leaveType.getRequireReason()) &&
                (requestDTO.getLeaveRequestReason() == null || requestDTO.getLeaveRequestReason().trim().isEmpty())) {
            throw new IllegalStateException("Reason is required for " + leaveType.getName() + " leave type");
        }

        // Validate document requirement (now based on documents list)
        if (Boolean.TRUE.equals(leaveType.getRequireDocument()) &&
                (requestDTO.getDocuments() == null || requestDTO.getDocuments().isEmpty())) {
            throw new IllegalStateException(
                    "Document attachment is required for " + leaveType.getName() + " leave type");
        }

        // Calculate duration in days
        long durationInDays = ChronoUnit.DAYS.between(requestDTO.getStartDate(), requestDTO.getEndDate()) + 1;

        // Adjust for half days
        if (Boolean.TRUE.equals(requestDTO.getHalfDayStart())) {
            durationInDays -= 0.5;
        }
        if (Boolean.TRUE.equals(requestDTO.getHalfDayEnd())) {
            durationInDays -= 0.5;
        }

        // Leave balance will be checked and updated during approval process

        // // Real-Time Balance Update: Subtract leave days from employee's leave
        // balance
        // try {
        // EmployeeBalance balance = employeeBalanceService.getBalance(employee.getId(),
        // leaveType.getId());
        // java.math.BigDecimal currentBalance = balance.getCurrentBalance();
        // java.math.BigDecimal leaveDays =
        // java.math.BigDecimal.valueOf(durationInDays);
        // java.math.BigDecimal newBalance = currentBalance.subtract(leaveDays);
        // if (newBalance.compareTo(java.math.BigDecimal.ZERO) < 0) {
        // throw new IllegalStateException("Insufficient leave balance. Your current
        // balance is "
        // + currentBalance.stripTrailingZeros().toPlainString() + " days, but you
        // requested "
        // + leaveDays.stripTrailingZeros().toPlainString() + " days.");
        // }
        // employeeBalanceService.updateBalance(balance.getId(), newBalance);
        // } catch (jakarta.persistence.EntityNotFoundException e) {
        // // If no balance exists for this leave type, treat as zero
        // throw new IllegalStateException("No leave balance found for this leave type.
        // Please contact HR.");
        // }

        // Validate duration against leave type's max duration
        if (leaveType.getMaxDuration() != null && durationInDays > leaveType.getMaxDuration()) {
            throw new IllegalStateException(
                    String.format(
                            "Leave request duration (%s days) exceeds maximum allowed duration (%s days) for leave type %s",
                            durationInDays, leaveType.getMaxDuration(), leaveType.getName()));
        }

        // Create leave request
        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setEmployee(employee);
        leaveRequest.setLeaveType(leaveType);
        leaveRequest.setStartDate(requestDTO.getStartDate());
        leaveRequest.setEndDate(requestDTO.getEndDate());
        leaveRequest.setHalfDayStart(requestDTO.getHalfDayStart());
        leaveRequest.setHalfDayEnd(requestDTO.getHalfDayEnd());
        leaveRequest.setLeaveRequestReason(requestDTO.getLeaveRequestReason());
        leaveRequest.setStatus(LeaveRequestStatus.PENDING);

        // Set primary document if provided
        if (requestDTO.getPrimaryDocument() != null) {
            DocumentDTO docDto = requestDTO.getPrimaryDocument();
            Document doc = new Document();
            doc.setName(docDto.getName());
            doc.setBlobUrl(docDto.getBlobUrl());
            doc.setVisible(docDto.getVisible() != null ? docDto.getVisible() : true);
            doc.setUploadedAt(java.time.LocalDateTime.now());
            doc.setLeaveRequest(leaveRequest);
            documentRepository.save(doc);
            leaveRequest.setPrimaryDocument(doc);
        }

        // Set the manager as the approver for pending requests
        if (employee.getManager() != null) {
            leaveRequest.setManager(employee.getManager());

            // Send notification to manager about new leave request
            String title = "New Leave Request";
            String message = String.format("%s has submitted a %s request from %s to %s.",
                    employee.getFullName(), leaveType.getName(),
                    requestDTO.getStartDate(), requestDTO.getEndDate());

            try {
                notificationService.sendNotification(employee.getManager(), title, message);
            } catch (Exception e) {
                log.error("Failed to send real-time notification to manager for leave request: {}", e.getMessage());
                // Don't throw the exception - notification failure shouldn't affect the
                // submission process
            }
        } else {
            log.warn("Employee {} has no manager assigned", employee.getId());
        }

        // Save leave request first to get the ID for document linkage
        leaveRequest = leaveRequestRepository.save(leaveRequest);

        // Save additional documents if provided
        if (requestDTO.getDocuments() != null) {
            for (DocumentDTO docDto : requestDTO.getDocuments()) {
                if (docDto.getBlobUrl() != null && !docDto.getBlobUrl().isEmpty()) {
                    Document doc = new Document();
                    doc.setName(docDto.getName());
                    doc.setBlobUrl(docDto.getBlobUrl());
                    doc.setVisible(docDto.getVisible() != null ? docDto.getVisible() : true);
                    doc.setUploadedAt(java.time.LocalDateTime.now());
                    doc.setLeaveRequest(leaveRequest);
                    documentRepository.save(doc);
                }
            }
        }

        // Send email notification
        try {
            emailService.sendLeaveRequestNotification(leaveRequest);
        } catch (Exception e) {
            log.error("Failed to send email notification for leave request {}: {}", leaveRequest.getId(),
                    e.getMessage());
            // Don't throw the exception - email failure shouldn't affect the submission
            // process
        }

        return leaveRequest;
    }

    @Transactional
    public LeaveRequest approveLeaveRequest(Long requestId, User approver, LeaveRequestApprovalDTO approvalDTO) {
        log.info("Processing leave request approval/rejection: {}", requestId);

        // Fetch leave request with necessary associations
        LeaveRequest leaveRequest = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Leave request not found with ID: " + requestId));

        // Initialize necessary associations
        Hibernate.initialize(leaveRequest.getEmployee());
        Hibernate.initialize(leaveRequest.getLeaveType());
        Hibernate.initialize(leaveRequest.getManager());

        // Check if approver is ADMIN or in the same department as the employee
        boolean isAdmin = approver.getRole().getName().equals("ROLE_ADMIN");
        boolean isSameDepartment = approver.getDepartment().getId()
                .equals(leaveRequest.getEmployee().getDepartment().getId());

        if (!isAdmin && !isSameDepartment) {
            throw new IllegalStateException("You are not authorized to approve this leave request");
        }

        // Update leave request status
        leaveRequest.setStatus(approvalDTO.getStatus());
        leaveRequest.setManager(approver);
        leaveRequest.setApprovedAt(LocalDateTime.now());

        if (approvalDTO.getStatus() == LeaveRequestStatus.REJECTED) {
            leaveRequest.setRejectionReason(approvalDTO.getRejectionReason());
        }

        LeaveRequest updatedRequest = leaveRequestRepository.save(leaveRequest);

        // Send email notification - handle failure gracefully
        try {
            emailService.sendLeaveRequestStatusNotification(updatedRequest);
        } catch (Exception e) {
            log.error("Failed to send email notification for leave request {}: {}", requestId, e.getMessage());
            // Don't throw the exception - email failure shouldn't affect the approval
            // process
        }

        // Send in-app notification based on the request status
        try {
            User employee = leaveRequest.getEmployee();
            LeaveType leaveType = leaveRequest.getLeaveType();

            if (approvalDTO.getStatus() == LeaveRequestStatus.APPROVED) {
                // Notify the employee about leave approval
                String title = "Leave Request Approved";
                String message = String.format("Your %s leave request from %s to %s has been approved.",
                        leaveType.getName(), leaveRequest.getStartDate(), leaveRequest.getEndDate());
                notificationService.sendNotification(employee, title, message);
            } else if (approvalDTO.getStatus() == LeaveRequestStatus.REJECTED) {
                // Notify the employee about leave rejection
                String title = "Leave Request Rejected";
                String message = String.format("Your %s leave request from %s to %s has been rejected. Reason: %s",
                        leaveType.getName(), leaveRequest.getStartDate(), leaveRequest.getEndDate(),
                        leaveRequest.getRejectionReason());
                notificationService.sendNotification(employee, title, message);
            }
        } catch (Exception e) {
            log.error("Failed to send in-app notification for leave request {}: {}", requestId, e.getMessage());
            // Don't throw the exception - notification failure shouldn't affect the
            // approval process
        }

        return updatedRequest;
    }

    @Transactional(readOnly = true)
    public Page<LeaveRequest> getLeaveRequestsByDepartment(Long departmentId, LeaveRequestStatus status,
            Pageable pageable) {
        if (status != null) {
            return leaveRequestRepository.findByDepartmentAndStatus(departmentId, status, pageable);
        }
        return leaveRequestRepository.findByDepartmentId(departmentId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<LeaveRequest> getLeaveRequestsByEmployee(User employee, Pageable pageable) {
        return leaveRequestRepository.findByEmployee(employee, pageable);
    }

    @Transactional(readOnly = true)
    public List<LeaveRequest> getLeaveRequestsByEmployeeAndStatus(User employee, LeaveRequestStatus status) {
        return leaveRequestRepository.findByEmployeeAndStatus(employee, status);
    }

    @Transactional(readOnly = true)
    public LeaveRequest getLeaveRequest(Long requestId) {
        return leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Leave request not found with ID: " + requestId));
    }

    @Transactional
    public LeaveRequestResponseDTO cancelLeaveRequest(Long requestId, User employee) {
        log.info("Cancelling leave request: {}", requestId);

        // Fetch leave request with minimal associations
        LeaveRequest leaveRequest = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Leave request not found with ID: " + requestId));

        // Validate employee owns the request
        if (!leaveRequest.getEmployee().getId().equals(employee.getId())) {
            throw new IllegalStateException("You are not authorized to cancel this leave request");
        }

        // Only pending requests can be cancelled
        if (leaveRequest.getStatus() != LeaveRequestStatus.PENDING) {
            throw new IllegalStateException("Only pending leave requests can be cancelled");
        }

        // Update status to cancelled
        leaveRequest.setStatus(LeaveRequestStatus.CANCELLED);
        leaveRequest = leaveRequestRepository.save(leaveRequest);

        // Send notification to the manager about the cancellation
        sendCancellationNotification(leaveRequest);

        // Map to DTO inside the transaction
        return LeaveRequestResponseDTO.fromEntity(leaveRequest);
    }

    private void sendCancellationNotification(LeaveRequest leaveRequest) {
        try {
            User manager = leaveRequest.getManager();
            if (manager != null) {
                String title = "Leave Request Cancelled";
                String message = String.format("%s has cancelled their %s leave request from %s to %s.",
                        leaveRequest.getEmployee().getFullName(),
                        leaveRequest.getLeaveType().getName(),
                        leaveRequest.getStartDate(),
                        leaveRequest.getEndDate());
                notificationService.sendNotification(manager, title, message);
            }
        } catch (Exception e) {
            log.error("Failed to send cancellation notification for request {}: {}", leaveRequest.getId(),
                    e.getMessage());
        }
    }

    // Mapper utility for LeaveRequest to LeaveRequestDTO
    private static class LeaveRequestMapper {
        public static LeaveRequestDTO toDTO(LeaveRequest lr) {
            // Extract all necessary data within the transaction
            String employeeName = null;
            String departmentName = null;
            String leaveTypeName = null;
            LeaveRequestStatus status = null;

            if (lr.getEmployee() != null) {
                employeeName = lr.getEmployee().getFullName();
                if (lr.getEmployee().getDepartment() != null) {
                    departmentName = lr.getEmployee().getDepartment().getName();
                }
            }

            if (lr.getLeaveType() != null) {
                leaveTypeName = lr.getLeaveType().getName();
            }

            status = lr.getStatus();

            // Create DTO with the extracted data
            return new LeaveRequestDTO(
                    lr.getId(),
                    employeeName,
                    departmentName,
                    leaveTypeName,
                    lr.getStartDate(),
                    lr.getEndDate(),
                    status);
        }
    }

    @Transactional(readOnly = true)
    public List<LeaveRequest> getApprovedLeaveRequestsByEmployeeAndLeaveType(User employee, LeaveType leaveType) {
        return leaveRequestRepository.findByEmployeeAndLeaveTypeAndStatus(
                employee, leaveType, LeaveRequestStatus.APPROVED);
    }

    /**
     * Get public holidays for a specific date range
     *
     * @param startDate the start date (inclusive)
     * @param endDate   the end date (inclusive)
     * @return list of public holidays in the date range
     */
    @Transactional(readOnly = true)
    public List<PublicHoliday> getPublicHolidaysForDateRange(LocalDate startDate, LocalDate endDate) {
        log.info("Fetching public holidays for date range {} to {}", startDate, endDate);
        return publicHolidayService.getHolidaysBetweenDates(startDate, endDate);
    }

    /**
     * Get company calendar data including all employee leave records and all public
     * holidays for an entire year.
     *
     * @param year  the year to get data for
     * @param month the month to get data for (optional, if provided will filter to
     *              just that month)
     * @return a DTO containing all employee leave records and all public holidays
     */
    @Transactional(readOnly = true)
    public CompanyCalendarDTO getCompanyCalendarData(int year, Integer month) {
        // Calculate date range - either for the whole year or for a specific month
        LocalDate firstDay;
        LocalDate lastDay;

        if (month != null) {
            // If month is provided, get data for that specific month
            firstDay = LocalDate.of(year, month, 1);
            lastDay = YearMonth.of(year, month).atEndOfMonth();
            log.info("Getting company calendar data for year: {}, month: {}, date range: {} to {}",
                    year, month, firstDay, lastDay);
        } else {
            // If month is not provided, get data for the entire year
            firstDay = LocalDate.of(year, 1, 1);
            lastDay = LocalDate.of(year, 12, 31);
            log.info("Getting company calendar data for entire year: {}, date range: {} to {}",
                    year, firstDay, lastDay);
        }

        // Get leave requests for all departments within the date range
        List<LeaveRequest> leaveRequests = leaveRequestRepository.findByDateRange(firstDay, lastDay);
        log.info("Fetching leave requests for ALL employees between {} and {}", firstDay, lastDay);

        // Convert leave requests to employee leave DTOs - only include APPROVED
        List<EmployeeLeaveDTO> employeeLeaves = leaveRequests.stream()
                .filter(request -> request.getStatus() == LeaveRequestStatus.APPROVED)
                .map(this::convertToEmployeeLeaveDTO)
                .collect(Collectors.toList());

        log.info("Found {} approved leave records for all employees in the requested period", employeeLeaves.size());

        // Get ALL public holidays for the specified year
        List<PublicHoliday> publicHolidays;
        if (month != null) {
            // If month is provided, get holidays for that month
            publicHolidays = publicHolidayService.getHolidaysBetweenDates(firstDay, lastDay);
        } else {
            // If no month provided, get all holidays for the year
            publicHolidays = publicHolidayService.getHolidaysByYear(year);
        }

        List<PublicHolidayDTO> publicHolidayDTOs = publicHolidays.stream()
                .map(PublicHolidayDTO::fromEntity)
                .collect(Collectors.toList());

        log.info("Retrieved {} public holidays from database", publicHolidayDTOs.size());

        // Get all departments
        List<Department> departments = departmentService.getAllDepartments();
        List<DepartmentDTO> departmentDTOs = departments.stream()
                .map(DepartmentDTO::fromEntity)
                .collect(Collectors.toList());

        log.info("Retrieved {} departments from database", departmentDTOs.size());

        // Create and return the response
        return CompanyCalendarDTO.builder()
                .employeeLeaves(employeeLeaves)
                .publicHolidays(publicHolidayDTOs)
                .departments(departmentDTOs)
                .build();
    }

    /**
     * Convert a LeaveRequest to an EmployeeLeaveDTO.
     *
     * @param leaveRequest the leave request to convert
     * @return the employee leave DTO
     */
    private EmployeeLeaveDTO convertToEmployeeLeaveDTO(LeaveRequest leaveRequest) {
        User employee = leaveRequest.getEmployee();
        Department department = employee.getDepartment();
        User manager = leaveRequest.getManager();

        // Calculate working days excluding weekends
        LocalDate startDate = leaveRequest.getStartDate();
        LocalDate endDate = leaveRequest.getEndDate();
        double days = 0;

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            // Skip weekends (Saturday = 6, Sunday = 7 in DayOfWeek enum)
            if (date.getDayOfWeek().getValue() < 6) {
                days += 1.0;
            }
        }

        // Adjust for half days
        if (Boolean.TRUE.equals(leaveRequest.getHalfDayStart()) && startDate.getDayOfWeek().getValue() < 6) {
            days -= 0.5;
        }
        if (Boolean.TRUE.equals(leaveRequest.getHalfDayEnd()) && endDate.getDayOfWeek().getValue() < 6) {
            days -= 0.5;
        }

        return EmployeeLeaveDTO.builder()
                .employeeId(employee.getId().toString())
                .employeeName(employee.getFullName())
                .departmentName(department != null ? department.getName() : "")
                .startDate(leaveRequest.getStartDate())
                .endDate(leaveRequest.getEndDate())
                .leaveType(leaveRequest.getLeaveType().getName())
                .status(leaveRequest.getStatus().name())
                .days(days)
                .approverId(manager != null ? manager.getId().toString() : null)
                .approverName(manager != null ? manager.getFullName() : null)
                .build();
    }

    @Transactional(readOnly = true)
    public Page<LeaveRequest> getDirectReportsLeaveRequests(User manager, LeaveRequestStatus status,
            Pageable pageable) {
        if (status != null) {
            return leaveRequestRepository.findByDepartmentAndStatus(manager.getDepartment().getId(), status, pageable);
        }
        return leaveRequestRepository.findByDepartmentId(manager.getDepartment().getId(), pageable);
    }

    @Transactional(readOnly = true)
    public Page<LeaveRequest> getLeaveRequestsToApprove(User manager, LeaveRequestStatus status, Pageable pageable) {
        if (status != null) {
            return leaveRequestRepository.findByDepartmentIdAndStatus(manager.getDepartment().getId(), status,
                    pageable);
        }
        return leaveRequestRepository.findByDepartmentId(manager.getDepartment().getId(), pageable);
    }

    @Transactional(readOnly = true)
    public Page<LeaveRequest> getLeaveRequestsByManagerId(Long managerId, LeaveRequestStatus status,
            Pageable pageable) {
        if (status != null) {
            return leaveRequestRepository.findByManagerIdAndStatus(managerId, status, pageable);
        }
        return leaveRequestRepository.findByManagerId(managerId, pageable);
    }
}
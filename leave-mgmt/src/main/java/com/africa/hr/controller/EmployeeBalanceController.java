package com.africa.hr.controller;

import com.africa.hr.dto.LeaveTypeStatsDTO;
import com.africa.hr.model.EmployeeBalance;
import com.africa.hr.model.LeaveRequest;
import com.africa.hr.model.User;
import com.africa.hr.service.EmployeeBalanceService;
import com.africa.hr.service.LeaveRequestService;
import com.africa.hr.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/balances")
@RequiredArgsConstructor
@Tag(name = "Employee Balance", description = "APIs for managing employee leave balances")
public class EmployeeBalanceController {

        private final EmployeeBalanceService employeeBalanceService;
        private final UserService userService;
        private final LeaveRequestService leaveRequestService;

        /**
         * Get detailed leave balance statistics for the authenticated employee.
         * Returns statistics for each leave type including total allowance, used days,
         * and remaining balance.
         * 
         * @param employee the authenticated user
         * @return List of leave type statistics
         */
        @GetMapping("/stats")
        @PreAuthorize("hasAnyRole('STAFF', 'MANAGER')")
        @Operation(summary = "Get detailed leave balance statistics", description = "Retrieves detailed leave balance statistics including used, total and remaining days for each leave type")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized - User is not authenticated"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<List<LeaveTypeStatsDTO>> getDetailedLeaveBalanceStats(
                        @Parameter(hidden = true) @AuthenticationPrincipal User employee) {
                log.info("Getting detailed leave balance statistics for employee {}", employee.getId());

                List<EmployeeBalance> balances = employeeBalanceService.findByEmployee(employee);

                List<LeaveTypeStatsDTO> stats = balances.stream()
                                .map(balance -> {
                                        LeaveTypeStatsDTO dto = new LeaveTypeStatsDTO();
                                        dto.setType(balance.getLeaveType().getName());
                                        dto.setTotal(balance.getTotalAllowance().doubleValue());

                                        // Calculate used days correctly - get used leave from leave requests
                                        BigDecimal totalAllowance = balance.getTotalAllowance();
                                        BigDecimal currentBalance = balance.getCurrentBalance();

                                        // Used days = days allocated minus days left
                                        // This calculation is only valid when the employee has already been allocated
                                        // their full leave allowance, which is the case for new employees with 0
                                        // balance
                                        BigDecimal used;

                                        // If current balance is zero but total allowance is also zero, there's nothing
                                        // to use
                                        if (totalAllowance.equals(BigDecimal.ZERO)) {
                                                used = BigDecimal.ZERO;
                                        } else {
                                                // Calculate actual used days from leave requests for this employee and
                                                // leave type
                                                List<LeaveRequest> approvedLeaves = leaveRequestService
                                                                .getApprovedLeaveRequestsByEmployeeAndLeaveType(
                                                                                balance.getEmployee(),
                                                                                balance.getLeaveType());

                                                if (approvedLeaves.isEmpty()) {
                                                        // If no approved leave requests, used days should be zero
                                                        used = BigDecimal.ZERO;
                                                } else {
                                                        // Sum up the days from all approved leave requests
                                                        used = approvedLeaves.stream()
                                                                        .map(LeaveRequest::getTotalDaysRequested)
                                                                        .map(BigDecimal::valueOf)
                                                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                                                }
                                        }

                                        dto.setUsed(used.doubleValue());
                                        dto.setDaysLeft(balance.getCurrentBalance().doubleValue());
                                        return dto;
                                })
                                .collect(Collectors.toList());

                return ResponseEntity.ok(stats);
        }

        /**
         * Get all leave balances for a specific employee.
         *
         * @param employeeId the ID of the employee
         * @return list of employee balances
         */
        @GetMapping("/employees/{employeeId}")
        @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
        @Operation(summary = "Get all leave balances for an employee", description = "Retrieves all leave balances for a specific employee. "
                        + "Only administrators and managers can access this endpoint.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Balances retrieved successfully"),
                        @ApiResponse(responseCode = "400", description = "Employee not found"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized - User is not authenticated"),
                        @ApiResponse(responseCode = "403", description = "Forbidden - User does not have required role"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<List<EmployeeBalance>> getEmployeeBalances(
                        @Parameter(description = "ID of the employee") @PathVariable @NotNull Long employeeId) {
                log.info("Getting balances for employee {}", employeeId);

                User employee = userService.findById(employeeId)
                                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));

                List<EmployeeBalance> balances = employeeBalanceService.findByEmployee(employee);
                return ResponseEntity.ok(balances);
        }

        /**
         * Get a specific leave balance for an employee.
         *
         * @param employeeId  the ID of the employee
         * @param leaveTypeId the ID of the leave type
         * @return the employee balance
         */
        @GetMapping("/employees/{employeeId}/leave-types/{leaveTypeId}")
        @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
        @Operation(summary = "Get a specific leave balance for an employee", description = "Retrieves a specific leave balance for an employee and leave type. "
                        + "Only administrators and managers can access this endpoint.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Balance retrieved successfully"),
                        @ApiResponse(responseCode = "400", description = "Employee or leave type not found"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized - User is not authenticated"),
                        @ApiResponse(responseCode = "403", description = "Forbidden - User does not have required role"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<EmployeeBalance> getEmployeeBalance(
                        @Parameter(description = "ID of the employee") @PathVariable @NotNull Long employeeId,
                        @Parameter(description = "ID of the leave type") @PathVariable @NotNull Long leaveTypeId) {
                log.info("Getting balance for employee {} and leave type {}", employeeId, leaveTypeId);

                EmployeeBalance balance = employeeBalanceService.getBalance(employeeId, leaveTypeId);
                return ResponseEntity.ok(balance);
        }
}
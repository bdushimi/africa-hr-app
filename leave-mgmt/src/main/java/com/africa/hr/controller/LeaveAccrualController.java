package com.africa.hr.controller;

import com.africa.hr.dto.LeaveAccrualResponseDTO;
import com.africa.hr.dto.LeaveAccrualSummaryDTO;
import com.africa.hr.model.EmployeeBalance;
import com.africa.hr.model.LeaveAccrual;
import com.africa.hr.model.User;
import com.africa.hr.service.EmployeeBalanceService;
import com.africa.hr.service.LeaveAccrualService;
import com.africa.hr.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/actions")
@RequiredArgsConstructor
@Tag(name = "Leave Accrual Actions", description = "APIs for managing leave accrual actions")
public class LeaveAccrualController {

        private final LeaveAccrualService leaveAccrualService;
        private final EmployeeBalanceService employeeBalanceService;
        private final UserService userService;

        /**
         * Process monthly accrual for a specific employee.
         * The accrual date is set to the 1st of the current month, but processes
         * accruals for the previous month.
         * 
         * For example:
         * - If called on April 4th, 2024 for employee 123:
         * - Accrual date is set to April 1st, 2024
         * - Processes March 2024 accruals for employee 123
         * - If called on May 2nd, 2024 for employee 123:
         * - Accrual date is set to May 1st, 2024
         * - Processes April 2024 accruals for employee 123
         *
         * @param employeeId the ID of the employee to process
         * @return list of processed accruals for the employee
         */
        @PostMapping("/processMonthlyAccruals/employees/{employeeId}")
        @PreAuthorize("hasRole('ADMIN')")
        @Operation(summary = "Process monthly leave accruals for a specific employee", description = "Processes leave accruals for a specific employee for the previous month. "
                        + "The accrual date is set to the 1st of the current month, "
                        + "but the system processes accruals for the previous month. "
                        + "For example, if called in April, it processes March accruals. "
                        + "Only administrators can trigger this process.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Accruals processed successfully"),
                        @ApiResponse(responseCode = "400", description = "Employee not found or not eligible for accrual"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized - User is not authenticated"),
                        @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role"),
                        @ApiResponse(responseCode = "409", description = "Accruals already processed for the employee for the previous month"),
                        @ApiResponse(responseCode = "500", description = "Internal server error during processing")
        })
        public ResponseEntity<List<LeaveAccrualResponseDTO>> processEmployeeMonthlyAccruals(
                        @Parameter(description = "ID of the employee to process") @PathVariable @NotNull Long employeeId) {

                // Set accrual date to 1st of current month
                LocalDate accrualDate = LocalDate.now().withDayOfMonth(1);

                // Calculate the month to process (previous month)
                YearMonth processMonth = YearMonth.from(accrualDate).minusMonths(1);

                log.info("Processing monthly accruals for employee {} - Accrual date: {}, Processing month: {}",
                                employeeId, accrualDate, processMonth);

                // Get employee
                User employee = userService.findById(employeeId)
                                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));

                // Get employee's balances
                List<EmployeeBalance> balances = employeeBalanceService.findByEmployee(employee);

                if (balances.isEmpty()) {
                        throw new IllegalArgumentException("Employee has no leave balances configured: " + employeeId);
                }

                // Check if employee has any accruals already processed for the month
                boolean hasProcessedAccruals = balances.stream()
                                .anyMatch(balance -> !leaveAccrualService.findByEmployeeBalanceAndYearAndMonth(
                                                balance,
                                                processMonth.getYear(),
                                                processMonth.getMonthValue())
                                                .isEmpty());

                if (hasProcessedAccruals) {
                        throw new IllegalStateException(
                                        "Accruals have already been processed for employee " + employeeId +
                                                        " for " + processMonth);
                }

                try {
                        // Process accruals for each balance
                        List<LeaveAccrual> processedAccruals = balances.stream()
                                        .map(balance -> leaveAccrualService.processAccrualForBalance(balance,
                                                        processMonth))
                                        .toList();

                        // Convert to DTOs
                        List<LeaveAccrualResponseDTO> responseDTOs = processedAccruals.stream()
                                        .map(accrual -> {
                                                LeaveAccrualResponseDTO dto = new LeaveAccrualResponseDTO();
                                                dto.setId(accrual.getId());
                                                dto.setEmployeeBalanceId(accrual.getEmployeeBalance().getId());
                                                dto.setEmployeeId(accrual.getEmployeeBalance().getEmployee().getId());
                                                dto.setEmployeeName(accrual.getEmployeeBalance().getEmployee()
                                                                .getFirstName() + " " +
                                                                accrual.getEmployeeBalance().getEmployee()
                                                                                .getLastName());
                                                dto.setLeaveTypeId(accrual.getEmployeeBalance().getLeaveType().getId());
                                                dto.setLeaveTypeName(
                                                                accrual.getEmployeeBalance().getLeaveType().getName());
                                                dto.setAccrualDate(accrual.getAccrualDate());
                                                dto.setAccrualPeriod(accrual.getYearMonth());
                                                dto.setAmount(accrual.getAmount());
                                                dto.setIsProrated(accrual.getIsProrated());
                                                dto.setCreatedAt(accrual.getCreatedAt());
                                                return dto;
                                        })
                                        .collect(Collectors.toList());

                        log.info("Successfully processed {} accruals for employee {} for month: {} (Accrual date: {})",
                                        processedAccruals.size(), employeeId, processMonth, accrualDate);

                        return ResponseEntity.ok(responseDTOs);
                } catch (Exception e) {
                        log.error("Error processing accruals for employee {} for month {} (Accrual date: {}): {}",
                                        employeeId, processMonth, accrualDate, e.getMessage(), e);
                        throw e;
                }
        }

        /**
         * Process monthly accruals for all employees.
         * The accrual date is set to the 1st of the current month, but processes
         * accruals for the previous month.
         * 
         * For example:
         * - If called on April 4th, 2024:
         * - Accrual date is set to April 1st, 2024
         * - Processes March 2024 accruals for all employees
         * - If called on May 2nd, 2024:
         * - Accrual date is set to May 1st, 2024
         * - Processes April 2024 accruals for all employees
         *
         * @return list of processed accruals for all employees
         */
        @PostMapping("/processMonthlyAccruals")
        @PreAuthorize("hasRole('ADMIN')")
        @Operation(summary = "Process leave accruals for all employees", description = "Processes monthly accruals for all eligible employees for the previous month. "
                        +
                        "Only administrators can trigger this process.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Accruals processed successfully"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized - User is not authenticated"),
                        @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role"),
                        @ApiResponse(responseCode = "500", description = "Internal server error during processing")
        })
        public ResponseEntity<List<LeaveAccrualResponseDTO>> processAllEmployeesMonthlyAccruals() {
                log.info("Processing monthly accruals for all employees");

                // Set accrual date to 1st of current month
                LocalDate accrualDate = LocalDate.now().withDayOfMonth(1);

                // Get previous month for processing
                YearMonth processMonth = YearMonth.from(accrualDate).minusMonths(1);

                try {
                        // Get all employees
                        List<User> allEmployees = userService.findAllEmployees();

                        // Process accruals for each employee's eligible balances
                        List<LeaveAccrual> processedAccruals = allEmployees.stream()
                                        .flatMap(employee -> {
                                                List<EmployeeBalance> balances = employeeBalanceService
                                                                .findByEmployee(employee);
                                                return balances.stream()
                                                                .filter(EmployeeBalance::isEligibleForAccrual)
                                                                .filter(balance -> leaveAccrualService
                                                                                .findByEmployeeBalanceAndYearAndMonth(
                                                                                                balance,
                                                                                                processMonth.getYear(),
                                                                                                processMonth.getMonthValue())
                                                                                .isEmpty())
                                                                .map(balance -> leaveAccrualService
                                                                                .processAccrualForBalance(balance,
                                                                                                processMonth));
                                        })
                                        .toList();

                        // Convert to DTOs
                        List<LeaveAccrualResponseDTO> responseDTOs = processedAccruals.stream()
                                        .map(accrual -> {
                                                LeaveAccrualResponseDTO dto = new LeaveAccrualResponseDTO();
                                                dto.setId(accrual.getId());
                                                dto.setEmployeeBalanceId(accrual.getEmployeeBalance().getId());
                                                dto.setEmployeeId(accrual.getEmployeeBalance().getEmployee().getId());
                                                dto.setEmployeeName(accrual.getEmployeeBalance().getEmployee()
                                                                .getFirstName() + " " +
                                                                accrual.getEmployeeBalance().getEmployee()
                                                                                .getLastName());
                                                dto.setLeaveTypeId(accrual.getEmployeeBalance().getLeaveType().getId());
                                                dto.setLeaveTypeName(
                                                                accrual.getEmployeeBalance().getLeaveType().getName());
                                                dto.setAccrualDate(accrual.getAccrualDate());
                                                dto.setAccrualPeriod(accrual.getYearMonth());
                                                dto.setAmount(accrual.getAmount());
                                                dto.setIsProrated(accrual.getIsProrated());
                                                dto.setCreatedAt(accrual.getCreatedAt());
                                                return dto;
                                        })
                                        .collect(Collectors.toList());

                        log.info("Successfully processed {} accruals for all employees for month: {} (Accrual date: {})",
                                        processedAccruals.size(), processMonth, accrualDate);

                        return ResponseEntity.ok(responseDTOs);

                } catch (Exception e) {
                        log.error("Error processing accruals for all employees for month {} (Accrual date: {}): {}",
                                        processMonth, accrualDate, e.getMessage(), e);
                        throw e;
                }
        }

        /**
         * Process accruals for a specific employee for the current year.
         * This endpoint processes monthly accruals for all eligible leave balances.
         *
         * @param employeeId the ID of the employee to process
         * @return list of processed accruals for the employee
         */
        @PostMapping("/processYearlyAccruals/employees/{employeeId}")
        @PreAuthorize("hasRole('ADMIN')")
        @Operation(summary = "Process leave accruals for a specific employee for the current year", description = "Processes monthly accruals for all eligible leave balances for the current year. "
                        + "Only administrators can trigger this process.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Accruals processed successfully"),
                        @ApiResponse(responseCode = "400", description = "Employee not found or not eligible for accrual"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized - User is not authenticated"),
                        @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role"),
                        @ApiResponse(responseCode = "500", description = "Internal server error during processing")
        })
        public ResponseEntity<List<LeaveAccrualResponseDTO>> processEmployeeAccrualsForCurrentYear(
                        @Parameter(description = "ID of the employee to process") @PathVariable @NotNull Long employeeId) {

                int currentYear = LocalDate.now().getYear();
                log.info("Processing accruals for employee {} for current year {}", employeeId, currentYear);

                // Get employee
                User employee = userService.findById(employeeId)
                                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));

                // Get employee's balances
                List<EmployeeBalance> balances = employeeBalanceService.findByEmployee(employee);

                if (balances.isEmpty()) {
                        throw new IllegalArgumentException("Employee has no leave balances configured: " + employeeId);
                }

                // Process accruals for each eligible balance for each month
                List<LeaveAccrual> processedAccruals = balances.stream()
                                .filter(balance -> balance.isEligibleForAccrual())
                                .flatMap(balance -> {
                                        // Process each month of the year
                                        return List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12).stream()
                                                        .map(month -> {
                                                                YearMonth yearMonth = YearMonth.of(currentYear, month);
                                                                return leaveAccrualService.processAccrualForBalance(
                                                                                balance, yearMonth);
                                                        });
                                })
                                .toList();

                if (processedAccruals.isEmpty()) {
                        log.info("No accruals processed for employee {} - No eligible balances", employeeId);
                        return ResponseEntity.ok(List.of());
                }

                // Convert to DTOs
                List<LeaveAccrualResponseDTO> responseDTOs = processedAccruals.stream()
                                .map(accrual -> {
                                        LeaveAccrualResponseDTO dto = new LeaveAccrualResponseDTO();
                                        dto.setId(accrual.getId());
                                        dto.setEmployeeBalanceId(accrual.getEmployeeBalance().getId());
                                        dto.setEmployeeId(accrual.getEmployeeBalance().getEmployee().getId());
                                        dto.setEmployeeName(accrual.getEmployeeBalance().getEmployee().getFirstName()
                                                        + " " +
                                                        accrual.getEmployeeBalance().getEmployee().getLastName());
                                        dto.setLeaveTypeId(accrual.getEmployeeBalance().getLeaveType().getId());
                                        dto.setLeaveTypeName(accrual.getEmployeeBalance().getLeaveType().getName());
                                        dto.setYearMonth(accrual.getYearMonth());
                                        dto.setAmount(accrual.getAmount());
                                        dto.setIsProrated(accrual.getIsProrated());
                                        dto.setCreatedAt(accrual.getCreatedAt());
                                        return dto;
                                })
                                .collect(Collectors.toList());

                log.info("Successfully processed {} accruals for employee {} for current year {}",
                                processedAccruals.size(), employeeId, currentYear);

                return ResponseEntity.ok(responseDTOs);
        }

        /**
         * Process leave accruals for all employees for the current year.
         *
         * @return list of processed accruals
         */
        @PostMapping("/employees/processYearlyAccruals")
        @PreAuthorize("hasRole('ADMIN')")
        @Operation(summary = "Process leave accruals for all employees", description = "Processes leave accruals for all employees for the current year")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Accruals processed successfully"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized - User is not authenticated"),
                        @ApiResponse(responseCode = "403", description = "Forbidden - User does not have required role"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<List<LeaveAccrualResponseDTO>> processAllEmployeeAccrualsForYear() {
                int currentYear = LocalDate.now().getYear();
                log.info("Processing leave accruals for all employees for current year {}", currentYear);

                // Get all employees
                List<User> employees = userService.findAllEmployees();

                if (employees.isEmpty()) {
                        log.info("No employees found to process accruals");
                        return ResponseEntity.ok(List.of());
                }

                // Process accruals for each employee
                List<LeaveAccrual> processedAccruals = employees.stream()
                                .flatMap(employee -> {
                                        List<EmployeeBalance> balances = employeeBalanceService
                                                        .findByEmployee(employee);

                                        return balances.stream()
                                                        .filter(EmployeeBalance::isEligibleForAccrual)
                                                        .flatMap(balance -> {
                                                                // Process each month of the year
                                                                return List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12)
                                                                                .stream()
                                                                                .map(month -> {
                                                                                        YearMonth yearMonth = YearMonth
                                                                                                        .of(currentYear, month);
                                                                                        return leaveAccrualService
                                                                                                        .processAccrualForBalance(
                                                                                                                        balance,
                                                                                                                        yearMonth);
                                                                                });
                                                        });
                                })
                                .toList();

                if (processedAccruals.isEmpty()) {
                        log.info("No accruals processed for any employees for current year {}", currentYear);
                        return ResponseEntity.ok(List.of());
                }

                // Convert to DTOs
                List<LeaveAccrualResponseDTO> responseDTOs = processedAccruals.stream()
                                .map(accrual -> {
                                        LeaveAccrualResponseDTO dto = new LeaveAccrualResponseDTO();
                                        dto.setId(accrual.getId());
                                        dto.setEmployeeBalanceId(accrual.getEmployeeBalance().getId());
                                        dto.setEmployeeId(accrual.getEmployeeBalance().getEmployee().getId());
                                        dto.setEmployeeName(accrual.getEmployeeBalance().getEmployee().getFirstName()
                                                        + " " +
                                                        accrual.getEmployeeBalance().getEmployee().getLastName());
                                        dto.setLeaveTypeId(accrual.getEmployeeBalance().getLeaveType().getId());
                                        dto.setLeaveTypeName(accrual.getEmployeeBalance().getLeaveType().getName());
                                        dto.setYearMonth(accrual.getYearMonth());
                                        dto.setAmount(accrual.getAmount());
                                        dto.setIsProrated(accrual.getIsProrated());
                                        dto.setCreatedAt(accrual.getCreatedAt());
                                        return dto;
                                })
                                .collect(Collectors.toList());

                log.info("Successfully processed {} accruals across all employees for current year {}",
                                processedAccruals.size(), currentYear);

                return ResponseEntity.ok(responseDTOs);
        }

        /**
         * Get accrual history for all employees.
         * This endpoint returns all accrual records, ordered by date descending.
         *
         * @return list of accrual records
         */
        @GetMapping("/leaveAccruals/history")
        public ResponseEntity<List<LeaveAccrualResponseDTO>> getAccrualHistory() {
                log.info("Fetching accrual history for all employees");

                List<LeaveAccrual> accruals = leaveAccrualService.findAllAccruals();

                List<LeaveAccrualResponseDTO> response = accruals.stream()
                                .map(accrual -> {
                                        LeaveAccrualResponseDTO dto = new LeaveAccrualResponseDTO();
                                        dto.setId(accrual.getId());
                                        dto.setEmployeeBalanceId(accrual.getEmployeeBalance().getId());
                                        dto.setEmployeeId(accrual.getEmployeeBalance().getEmployee().getId());
                                        dto.setEmployeeName(accrual.getEmployeeBalance().getEmployee().getFirstName()
                                                        + " " +
                                                        accrual.getEmployeeBalance().getEmployee().getLastName());
                                        dto.setLeaveTypeId(accrual.getEmployeeBalance().getLeaveType().getId());
                                        dto.setLeaveTypeName(accrual.getEmployeeBalance().getLeaveType().getName());
                                        dto.setYearMonth(accrual.getYearMonth());
                                        dto.setAmount(accrual.getAmount());
                                        dto.setIsProrated(accrual.getIsProrated());
                                        dto.setCreatedAt(accrual.getCreatedAt());
                                        dto.setAccrualDate(accrual.getAccrualDate());
                                        dto.setAccrualPeriod(accrual.getYearMonth());
                                        return dto;
                                })
                                .toList();

                return ResponseEntity.ok(response);
        }

        /**
         * Get accrual history summary for all periods.
         * Returns a summary of accruals processed for each period.
         *
         * @return list of accrual summaries
         */
        @GetMapping("/leaveAccruals/history/summary")
        @Operation(summary = "Get accrual history summary", description = "Returns a summary of accruals processed for each period")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved accrual history summary"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<List<LeaveAccrualSummaryDTO>> getAccrualHistorySummary() {
                log.info("Fetching accrual history summary");
                List<LeaveAccrualSummaryDTO> summary = leaveAccrualService.getAccrualHistorySummary();
                return ResponseEntity.ok(summary);
        }

        /**
         * Get detailed accrual records for a specific period.
         *
         * @param periodId the ID of the accrual period
         * @return list of accrual details
         */
        @GetMapping("/leaveAccruals/history/{periodId}/details")
        @Operation(summary = "Get accrual details", description = "Returns detailed accrual records for a specific period")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved accrual details"),
                        @ApiResponse(responseCode = "404", description = "Accrual period not found"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<List<LeaveAccrualResponseDTO>> getAccrualDetails(
                        @Parameter(description = "ID of the accrual period") @PathVariable Long periodId) {
                log.info("Fetching accrual details for period ID: {}", periodId);

                List<LeaveAccrual> accruals = leaveAccrualService.getAccrualDetails(periodId);

                List<LeaveAccrualResponseDTO> response = accruals.stream()
                                .map(accrual -> {
                                        LeaveAccrualResponseDTO dto = new LeaveAccrualResponseDTO();
                                        dto.setId(accrual.getId());
                                        dto.setEmployeeBalanceId(accrual.getEmployeeBalance().getId());
                                        dto.setEmployeeId(accrual.getEmployeeBalance().getEmployee().getId());
                                        dto.setEmployeeName(accrual.getEmployeeBalance().getEmployee().getFirstName()
                                                        + " " +
                                                        accrual.getEmployeeBalance().getEmployee().getLastName());
                                        dto.setLeaveTypeId(accrual.getEmployeeBalance().getLeaveType().getId());
                                        dto.setLeaveTypeName(accrual.getEmployeeBalance().getLeaveType().getName());
                                        dto.setYearMonth(accrual.getYearMonth());
                                        dto.setAmount(accrual.getAmount());
                                        dto.setIsProrated(accrual.getIsProrated());
                                        dto.setCreatedAt(accrual.getCreatedAt());
                                        dto.setAccrualDate(accrual.getAccrualDate());
                                        dto.setAccrualPeriod(accrual.getYearMonth());
                                        return dto;
                                })
                                .toList();

                return ResponseEntity.ok(response);
        }
}
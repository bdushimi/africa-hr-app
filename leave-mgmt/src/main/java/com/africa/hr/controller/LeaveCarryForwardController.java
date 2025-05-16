package com.africa.hr.controller;

import com.africa.hr.dto.LeaveCarryForwardResponseDTO;
import com.africa.hr.model.EmployeeBalance;
import com.africa.hr.model.LeaveCarryForward;
import com.africa.hr.model.User;
import com.africa.hr.service.EmployeeBalanceService;
import com.africa.hr.service.LeaveAccrualService;
import com.africa.hr.service.LeaveCarryForwardService;
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
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/actions")
@RequiredArgsConstructor
@Tag(name = "Leave Carry Forward Actions", description = "APIs for managing leave carry forward actions")
public class LeaveCarryForwardController {

        private final LeaveCarryForwardService carryForwardService;
        private final LeaveAccrualService accrualService;
        private final EmployeeBalanceService employeeBalanceService;
        private final UserService userService;

        /**
         * Process carry-forward for a specific employee.
         * This endpoint:
         * 1. First processes all accruals for the current year
         * 2. Then processes carry-forward from current year to next year
         *
         * @param employeeId the ID of the employee to process
         * @return list of carry-forward records created
         */
        @PostMapping("/employees/{employeeId}")
        @PreAuthorize("hasRole('ADMIN')")
        @Operation(summary = "Process leave carry-forward for a specific employee", description = "First processes all accruals for the current year, then processes carry-forward from current year to next year. "
                        + "Only administrators can trigger this process.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Carry-forward processed successfully"),
                        @ApiResponse(responseCode = "400", description = "Employee not found or not eligible for carry-forward"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized - User is not authenticated"),
                        @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role"),
                        @ApiResponse(responseCode = "500", description = "Internal server error during processing")
        })
        public ResponseEntity<List<LeaveCarryForwardResponseDTO>> processEmployeeCarryForward(
                        @Parameter(description = "ID of the employee to process") @PathVariable @NotNull Long employeeId) {

                // Calculate years for carry-forward
                int currentYear = LocalDate.now().getYear();
                int fromYear = currentYear; // Current year
                int toYear = currentYear + 1; // Next year
                LocalDate today = LocalDate.now();

                log.info("Processing carry-forward for employee {} from {} to {}",
                                employeeId, fromYear, toYear);

                // Get employee
                User employee = userService.findById(employeeId)
                                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));

                // Check if employee has joined
                if (employee.getJoinedDate().isAfter(LocalDate.now())) {
                        throw new IllegalStateException(
                                        "Cannot process carry-forward for employee " + employeeId +
                                                        " - Employee has not joined yet. Joining date: "
                                                        + employee.getJoinedDate());
                }

                // Get employee's balances
                List<EmployeeBalance> balances = employeeBalanceService.findByEmployee(employee);

                if (balances.isEmpty()) {
                        throw new IllegalArgumentException("Employee has no leave balances configured: " + employeeId);
                }

                // Check if carry-forward has already been processed for this year
                boolean hasProcessedCarryForward = balances.stream()
                                .filter(balance -> balance.getLeaveType().getIsCarryForwardEnabled())
                                .anyMatch(balance -> !carryForwardService
                                                .findByEmployeeBalanceAndFromYear(balance, fromYear).isEmpty());

                if (hasProcessedCarryForward) {
                        throw new IllegalStateException(
                                        "Carry-forward has already been processed for employee " + employeeId +
                                                        " from " + fromYear + " to " + toYear);
                }

                // Step 1: Process accruals for the current year up to today
                log.info("Processing accruals for employee {} for year {} up to {}", employeeId, fromYear, today);
                balances.stream()
                                .filter(balance -> balance.isEligibleForAccrual())
                                .forEach(balance -> {
                                        // Process each month of the current year up to today
                                        for (int month = 1; month <= today.getMonthValue(); month++) {
                                                YearMonth yearMonth = YearMonth.of(fromYear, month);
                                                accrualService.processAccrualForBalance(balance, yearMonth);
                                        }
                                });

                // Step 1.5: Add projected accruals for remaining months of the year
                log.info("Adding projected accruals for employee {} for remaining months of {}", employeeId, fromYear);
                balances.stream()
                                .filter(balance -> balance.isEligibleForAccrual())
                                .forEach(balance -> {
                                        // Process remaining months of the current year
                                        for (int month = today.getMonthValue() + 1; month <= 12; month++) {
                                                YearMonth yearMonth = YearMonth.of(fromYear, month);
                                                BigDecimal projectedAccrual = balance.getLeaveType().getAccrualRate();
                                                balance.setCurrentBalance(
                                                                balance.getCurrentBalance().add(projectedAccrual));
                                        }
                                });

                // Step 2: Process carry-forward to next year
                List<LeaveCarryForward> carryForwards = balances.stream()
                                .filter(balance -> balance.getLeaveType().getIsCarryForwardEnabled())
                                .map(balance -> carryForwardService.processCarryForward(balance, fromYear, toYear))
                                .filter(Optional::isPresent)
                                .map(Optional::get)
                                .toList();

                if (carryForwards.isEmpty()) {
                        log.info("No carry-forwards processed for employee {} - No eligible balances", employeeId);
                        return ResponseEntity.ok(List.of());
                }

                // Convert to DTOs
                List<LeaveCarryForwardResponseDTO> responseDTOs = carryForwards.stream()
                                .map(carryForward -> {
                                        LeaveCarryForwardResponseDTO dto = new LeaveCarryForwardResponseDTO();
                                        dto.setId(carryForward.getId());
                                        dto.setEmployeeBalanceId(carryForward.getEmployeeBalance().getId());
                                        dto.setEmployeeId(carryForward.getEmployeeBalance().getEmployee().getId());
                                        dto.setEmployeeName(carryForward.getEmployeeBalance().getEmployee()
                                                        .getFirstName() + " " +
                                                        carryForward.getEmployeeBalance().getEmployee().getLastName());
                                        dto.setLeaveTypeId(carryForward.getEmployeeBalance().getLeaveType().getId());
                                        dto.setLeaveTypeName(
                                                        carryForward.getEmployeeBalance().getLeaveType().getName());
                                        dto.setFromYear(carryForward.getFromYear());
                                        dto.setToYear(carryForward.getToYear());

                                        // Calculate carry-forward details
                                        BigDecimal currentBalance = carryForward.getEmployeeBalance()
                                                        .getCurrentBalance();
                                        BigDecimal carryForwardCap = carryForward.getEmployeeBalance().getLeaveType()
                                                        .getCarryForwardCap();
                                        BigDecimal monthlyAccrual = carryForward.getEmployeeBalance().getLeaveType()
                                                        .getAccrualRate();
                                        LocalDate joiningDate = carryForward.getEmployeeBalance().getEmployee()
                                                        .getJoinedDate();

                                        // Step 1: Calculate actual accruals (joining date to current date)
                                        BigDecimal actualAccruals = BigDecimal.ZERO;

                                        // Calculate prorated accrual for joining month
                                        YearMonth joiningMonth = YearMonth.from(joiningDate);
                                        int daysInJoiningMonth = joiningMonth.lengthOfMonth();
                                        int daysWorkedInJoiningMonth = daysInJoiningMonth - joiningDate.getDayOfMonth()
                                                        + 1;
                                        BigDecimal joiningMonthAccrual = monthlyAccrual
                                                        .multiply(BigDecimal.valueOf(daysWorkedInJoiningMonth))
                                                        .divide(BigDecimal.valueOf(daysInJoiningMonth), 2,
                                                                        RoundingMode.HALF_UP);
                                        actualAccruals = actualAccruals.add(joiningMonthAccrual);

                                        // Calculate full months between joining and current date
                                        int fullMonths = today.getMonthValue() - joiningDate.getMonthValue() - 1;
                                        if (fullMonths > 0) {
                                                actualAccruals = actualAccruals.add(monthlyAccrual
                                                                .multiply(BigDecimal.valueOf(fullMonths)));
                                        }

                                        // Calculate prorated accrual for current month
                                        YearMonth currentMonth = YearMonth.from(today);
                                        int daysInCurrentMonth = currentMonth.lengthOfMonth();
                                        int daysWorkedInCurrentMonth = today.getDayOfMonth();
                                        BigDecimal currentMonthAccrual = monthlyAccrual
                                                        .multiply(BigDecimal.valueOf(daysWorkedInCurrentMonth))
                                                        .divide(BigDecimal.valueOf(daysInCurrentMonth), 2,
                                                                        RoundingMode.HALF_UP);
                                        actualAccruals = actualAccruals.add(currentMonthAccrual);

                                        // Step 2: Calculate projected accruals (current date to end of year)
                                        BigDecimal projectedAccruals = BigDecimal.ZERO;

                                        // Calculate prorated accrual for remaining days in current month
                                        int remainingDaysInCurrentMonth = daysInCurrentMonth - daysWorkedInCurrentMonth;
                                        BigDecimal remainingCurrentMonthAccrual = monthlyAccrual
                                                        .multiply(BigDecimal.valueOf(remainingDaysInCurrentMonth))
                                                        .divide(BigDecimal.valueOf(daysInCurrentMonth), 2,
                                                                        RoundingMode.HALF_UP);
                                        projectedAccruals = projectedAccruals.add(remainingCurrentMonthAccrual);

                                        // Calculate full months remaining in the year
                                        int remainingFullMonths = 12 - today.getMonthValue();
                                        if (remainingFullMonths > 0) {
                                                projectedAccruals = projectedAccruals.add(monthlyAccrual
                                                                .multiply(BigDecimal.valueOf(remainingFullMonths)));
                                        }

                                        // Step 3: Calculate total balance for the year
                                        BigDecimal totalBalance = actualAccruals.add(projectedAccruals);

                                        // Step 4: Calculate carry-forward amounts
                                        BigDecimal amountToBeCarriedForward = totalBalance.min(carryForwardCap);
                                        BigDecimal amountToBeForfeited = totalBalance
                                                        .subtract(amountToBeCarriedForward);

                                        // Set all amounts
                                        dto.setOriginalBalance(totalBalance);
                                        dto.setCurrentBalance(amountToBeCarriedForward); // After carry-forward
                                        dto.setCarryForwardCap(carryForwardCap);
                                        dto.setAmountToBeCarriedForward(amountToBeCarriedForward);
                                        dto.setAmountToBeForfeited(amountToBeForfeited);

                                        dto.setCreatedAt(carryForward.getCreatedAt());
                                        return dto;
                                })
                                .collect(Collectors.toList());

                log.info("Successfully processed {} carry-forwards for employee {} from {} to {}",
                                carryForwards.size(), employeeId, fromYear, toYear);

                return ResponseEntity.ok(responseDTOs);
        }
}
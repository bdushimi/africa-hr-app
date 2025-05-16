package com.africa.hr.service;

import com.africa.hr.dto.LeaveAccrualSummaryDTO;
import com.africa.hr.model.EmployeeBalance;
import com.africa.hr.model.LeaveAccrual;
import com.africa.hr.repository.LeaveAccrualRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for handling leave accrual calculations and processing.
 * Manages monthly accrual calculations, prorated accruals, and accrual history.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveAccrualService {

    private final EmployeeBalanceService employeeBalanceService;
    private final LeaveTypeService leaveTypeService;
    private final LeaveAccrualRepository leaveAccrualRepository;

    /**
     * Process accrual for a specific employee's balance.
     *
     * @param balance   the employee balance to process
     * @param yearMonth the year and month to process accrual for
     * @return the processed accrual record
     */
    @Transactional
    public LeaveAccrual processAccrualForBalance(EmployeeBalance balance, YearMonth yearMonth) {
        log.info("Processing accrual for employee {} and leave type {} for {}",
                balance.getEmployee().getId(),
                balance.getLeaveType().getId(),
                yearMonth);

        // Calculate accrual amount
        BigDecimal accrualAmount = calculateAccrualAmount(balance, yearMonth);

        // Create accrual record
        LeaveAccrual accrual = new LeaveAccrual();
        accrual.setEmployeeBalance(balance);
        accrual.setAccrualDate(LocalDate.now());
        accrual.setAmount(accrualAmount);
        accrual.setYearMonth(yearMonth);
        accrual.setIsProrated(isProratedAccrual(balance, yearMonth));

        // Update balance
        employeeBalanceService.adjustBalance(balance.getId(), accrualAmount);
        balance.setLastAccrualDate(LocalDate.now());

        // Save accrual record
        return leaveAccrualRepository.save(accrual);
    }

    /**
     * Calculate the accrual amount for an employee's balance.
     * Handles prorated calculations for new employees.
     *
     * @param balance   the employee balance
     * @param yearMonth the year and month to calculate for
     * @return the calculated accrual amount
     */
    private BigDecimal calculateAccrualAmount(EmployeeBalance balance, YearMonth yearMonth) {
        if (!balance.isEligibleForAccrual()) {
            return BigDecimal.ZERO;
        }

        LocalDate monthStart = yearMonth.atDay(1);
        LocalDate monthEnd = yearMonth.atEndOfMonth();

        // If employee joined before the month, return full accrual
        if (balance.getEmployee().getJoinedDate().isBefore(monthStart)) {
            return balance.getLeaveType().getAccrualRate();
        }

        // If employee joined after the month, return zero
        if (balance.getEmployee().getJoinedDate().isAfter(monthEnd)) {
            return BigDecimal.ZERO;
        }

        // Calculate prorated accrual for mid-month join
        return balance.calculateProratedAccrual(monthStart, monthEnd);
    }

    /**
     * Check if an accrual should be prorated.
     *
     * @param balance   the employee balance
     * @param yearMonth the year and month to check
     * @return true if the accrual should be prorated
     */
    private boolean isProratedAccrual(EmployeeBalance balance, YearMonth yearMonth) {
        LocalDate monthStart = yearMonth.atDay(1);
        return balance.getEmployee().getJoinedDate().isAfter(monthStart.minusDays(1))
                && balance.getEmployee().getJoinedDate().isBefore(yearMonth.atEndOfMonth().plusDays(1));
    }

    /**
     * Find accruals for an employee balance.
     *
     * @param balance the employee balance
     * @return list of accruals
     */
    public List<LeaveAccrual> findByEmployeeBalance(EmployeeBalance balance) {
        return leaveAccrualRepository.findByEmployeeBalanceOrderByAccrualDateDesc(balance);
    }

    /**
     * Find accruals for an employee balance in a specific year.
     *
     * @param balance the employee balance
     * @param year    the year to find accruals for
     * @return list of accruals
     */
    public List<LeaveAccrual> findByEmployeeBalanceAndYear(EmployeeBalance balance, Integer year) {
        return leaveAccrualRepository.findByEmployeeBalanceAndAccrualYearOrderByAccrualMonthDesc(balance, year);
    }

    /**
     * Find accruals for an employee balance in a specific year and month.
     *
     * @param balance the employee balance
     * @param year    the year to find accruals for
     * @param month   the month to find accruals for
     * @return list of accruals
     */
    public List<LeaveAccrual> findByEmployeeBalanceAndYearAndMonth(
            EmployeeBalance balance, Integer year, Integer month) {
        return leaveAccrualRepository.findByEmployeeBalanceAndAccrualYearAndAccrualMonth(
                balance, year, month);
    }

    /**
     * Check if accruals have already been processed for a specific month.
     *
     * @param year  the year to check
     * @param month the month to check (1-12)
     * @return true if accruals have been processed for the month
     */
    public boolean hasAccrualsBeenProcessed(int year, int month) {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Month must be between 1 and 12");
        }

        // Check if any accruals exist for this month
        return !leaveAccrualRepository.findByAccrualYearAndAccrualMonth(year, month).isEmpty();
    }

    /**
     * Find all accrual records, ordered by accrual date descending.
     *
     * @return list of all accrual records
     */
    @Transactional(readOnly = true)
    public List<LeaveAccrual> findAllAccruals() {
        return leaveAccrualRepository.findAll().stream()
                .sorted((a1, a2) -> a2.getAccrualDate().compareTo(a1.getAccrualDate()))
                .toList();
    }

    /**
     * Get accrual history summary grouped by accrual period.
     * Returns a summary of accruals processed for each period.
     *
     * @return list of accrual summaries
     */
    @Transactional(readOnly = true)
    public List<LeaveAccrualSummaryDTO> getAccrualHistorySummary() {
        log.info("Fetching accrual history summary");

        // Get all accruals ordered by date
        List<LeaveAccrual> allAccruals = findAllAccruals();

        // Group accruals by yearMonth
        Map<YearMonth, List<LeaveAccrual>> accrualsByPeriod = allAccruals.stream()
                .collect(Collectors.groupingBy(LeaveAccrual::getYearMonth));

        // Convert to summary DTOs
        return accrualsByPeriod.entrySet().stream()
                .map(entry -> {
                    YearMonth period = entry.getKey();
                    List<LeaveAccrual> periodAccruals = entry.getValue();

                    LeaveAccrualSummaryDTO summary = new LeaveAccrualSummaryDTO();
                    summary.setId(periodAccruals.get(0).getId()); // Use first accrual's ID as summary ID
                    summary.setAccrualPeriod(period.format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy")));
                    summary.setProcessedDate(periodAccruals.get(0).getCreatedAt());
                    summary.setEmployeeCount((int) periodAccruals.stream()
                            .map(accrual -> accrual.getEmployeeBalance().getEmployee().getId())
                            .distinct()
                            .count());
                    summary.setTotalAccruals(periodAccruals.size());

                    // Calculate total days accrued
                    BigDecimal totalDays = periodAccruals.stream()
                            .map(LeaveAccrual::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    summary.setTotalDaysAccrued(totalDays);

                    // Determine status
                    boolean hasProrated = periodAccruals.stream().anyMatch(LeaveAccrual::getIsProrated);
                    boolean hasFailed = periodAccruals.stream()
                            .anyMatch(accrual -> accrual.getAmount().compareTo(BigDecimal.ZERO) == 0);

                    if (hasFailed) {
                        summary.setStatus("FAILED");
                    } else if (hasProrated) {
                        summary.setStatus("PARTIAL");
                    } else {
                        summary.setStatus("COMPLETED");
                    }

                    return summary;
                })
                .sorted((a, b) -> b.getProcessedDate().compareTo(a.getProcessedDate()))
                .toList();
    }

    /**
     * Get detailed accrual records for a specific period.
     *
     * @param periodId the ID of the accrual period
     * @return list of accrual details
     */
    @Transactional(readOnly = true)
    public List<LeaveAccrual> getAccrualDetails(Long periodId) {
        log.info("Fetching accrual details for period ID: {}", periodId);

        // Get the accrual to find its period
        LeaveAccrual referenceAccrual = leaveAccrualRepository.findById(periodId)
                .orElseThrow(() -> new EntityNotFoundException("Accrual not found with ID: " + periodId));

        // Get all accruals for the same period
        return leaveAccrualRepository.findByYearMonth(referenceAccrual.getYearMonth())
                .stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();
    }
}
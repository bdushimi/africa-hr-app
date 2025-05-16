package com.africa.hr.service;

import com.africa.hr.model.EmployeeBalance;
import com.africa.hr.model.LeaveCarryForward;
import com.africa.hr.model.LeaveType;
import com.africa.hr.repository.EmployeeBalanceRepository;
import com.africa.hr.repository.LeaveCarryForwardRepository;
import com.africa.hr.repository.LeaveTypeRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveCarryForwardService {

    private final LeaveCarryForwardRepository carryForwardRepository;
    private final EmployeeBalanceRepository employeeBalanceRepository;
    private final LeaveTypeRepository leaveTypeRepository;

    /**
     * Process carry-forward for all eligible leave balances for a year transition.
     * This method should be called at the end of each year.
     *
     * @param fromYear the year to carry forward from
     * @param toYear   the year to carry forward to
     * @return list of processed carry-forwards
     */
    @Transactional
    public List<LeaveCarryForward> processAnnualCarryForward(Integer fromYear, Integer toYear) {
        log.info("Processing annual carry-forward from {} to {}", fromYear, toYear);

        // Get all leave types that have carry-forward enabled
        List<LeaveType> carryForwardTypes = leaveTypeRepository.findByIsCarryForwardEnabledTrue();

        // Get all employee balances for these leave types
        List<EmployeeBalance> balances = employeeBalanceRepository
                .findByLeaveTypeIn(carryForwardTypes);

        return balances.stream()
                .map(balance -> processCarryForward(balance, fromYear, toYear))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    /**
     * Process carry-forward for a specific employee balance.
     *
     * @param employeeBalance the employee balance to process
     * @param fromYear        the year to carry forward from
     * @param toYear          the year to carry forward to
     * @return optional containing the processed carry-forward if successful
     */
    @Transactional
    public Optional<LeaveCarryForward> processCarryForward(
            EmployeeBalance employeeBalance,
            Integer fromYear,
            Integer toYear) {

        // Check if carry-forward already exists for this year transition
        if (carryForwardRepository
                .findByEmployeeBalanceAndFromYearAndToYear(employeeBalance, fromYear, toYear)
                .isPresent()) {
            log.warn("Carry-forward already exists for employee {} and year transition {} to {}",
                    employeeBalance.getEmployee().getUsername(), fromYear, toYear);
            return Optional.empty();
        }

        LeaveType leaveType = employeeBalance.getLeaveType();

        // Validate carry-forward eligibility
        if (!leaveType.getIsCarryForwardEnabled()) {
            log.info("Carry-forward not enabled for leave type: {}", leaveType.getName());
            return Optional.empty();
        }

        // Calculate carry-forward amounts
        BigDecimal originalBalance = employeeBalance.getCurrentBalance();
        BigDecimal carryForwardCap = leaveType.getCarryForwardCap();

        // Calculate carried forward and forfeited amounts
        BigDecimal carriedForwardAmount = originalBalance.min(carryForwardCap);
        BigDecimal forfeitedAmount = originalBalance.subtract(carriedForwardAmount);

        // Create and save carry-forward record
        LeaveCarryForward carryForward = new LeaveCarryForward();
        carryForward.setEmployeeBalance(employeeBalance);
        carryForward.setFromYear(fromYear);
        carryForward.setToYear(toYear);
        carryForward.setCarryForwardDate(LocalDate.now());
        carryForward.setOriginalBalance(originalBalance);
        carryForward.setCarriedForwardAmount(carriedForwardAmount);
        carryForward.setForfeitedAmount(forfeitedAmount);

        // Update employee balance
        employeeBalance.setCurrentBalance(carriedForwardAmount);
        employeeBalanceRepository.save(employeeBalance);

        // Save carry-forward record
        LeaveCarryForward savedCarryForward = carryForwardRepository.save(carryForward);

        log.info("Processed carry-forward for employee {}: carried forward {} days, forfeited {} days",
                employeeBalance.getEmployee().getUsername(),
                carriedForwardAmount,
                forfeitedAmount);

        return Optional.of(savedCarryForward);
    }

    /**
     * Get carry-forward history for an employee balance.
     *
     * @param employeeBalance the employee balance
     * @return list of carry-forwards ordered by date descending
     */
    public List<LeaveCarryForward> getCarryForwardHistory(EmployeeBalance employeeBalance) {
        return carryForwardRepository
                .findByEmployeeBalanceOrderByCarryForwardDateDesc(employeeBalance);
    }

    /**
     * Get carry-forward history for a specific year transition.
     *
     * @param fromYear the year to carry forward from
     * @param toYear   the year to carry forward to
     * @return list of carry-forwards
     */
    public List<LeaveCarryForward> getCarryForwardHistory(Integer fromYear, Integer toYear) {
        return carryForwardRepository.findByFromYearAndToYear(fromYear, toYear);
    }

    /**
     * Get the total carried forward amount for an employee balance in a specific
     * year transition.
     *
     * @param employeeBalance the employee balance
     * @param fromYear        the year to carry forward from
     * @param toYear          the year to carry forward to
     * @return the total carried forward amount
     */
    public BigDecimal getTotalCarriedForward(
            EmployeeBalance employeeBalance,
            Integer fromYear,
            Integer toYear) {
        BigDecimal total = carryForwardRepository
                .getTotalCarriedForwardForYearTransition(employeeBalance, fromYear, toYear);
        return total != null ? total : BigDecimal.ZERO;
    }

    /**
     * Find carry-forwards for an employee balance from a specific year.
     *
     * @param balance  the employee balance
     * @param fromYear the year to find carry-forwards from
     * @return list of carry-forwards
     */
    public List<LeaveCarryForward> findByEmployeeBalanceAndFromYear(EmployeeBalance balance, int fromYear) {
        return carryForwardRepository.findByEmployeeBalanceAndFromYear(balance, fromYear);
    }
}
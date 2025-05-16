package com.africa.hr.service;

import com.africa.hr.model.EmployeeBalance;
import com.africa.hr.model.LeaveType;
import com.africa.hr.model.User;
import com.africa.hr.repository.EmployeeBalanceRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Service for managing employee leave balances.
 * Handles balance creation, updates, and validation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeBalanceService {

    private final EmployeeBalanceRepository employeeBalanceRepository;
    private final LeaveTypeService leaveTypeService;

    /**
     * Create a new employee balance.
     *
     * @param employee    the employee
     * @param leaveTypeId the leave type ID
     * @return the created balance
     * @throws IllegalStateException if a balance already exists or if the
     *                               configuration is invalid
     */
    @Transactional
    public EmployeeBalance createBalance(User employee, Long leaveTypeId) {
        log.info("Creating balance for employee {} and leave type {}", employee.getId(), leaveTypeId);

        LeaveType leaveType = leaveTypeService.getLeaveType(leaveTypeId);

        if (employeeBalanceRepository.existsByEmployeeAndLeaveType(employee, leaveType)) {
            throw new IllegalStateException(
                    "Balance already exists for employee " + employee.getId() + " and leave type " + leaveTypeId);
        }

        EmployeeBalance balance = new EmployeeBalance();
        balance.setEmployee(employee);
        balance.setLeaveType(leaveType);
        balance.setCurrentBalance(BigDecimal.ZERO);
        balance.setMaxBalance(leaveType.getMaxDuration() != null ? new BigDecimal(leaveType.getMaxDuration()) : null);
        balance.setIsEligibleForAccrual(employee.getStatus() == User.Status.ACTIVE && leaveType.getAccrualBased());

        balance.validateBalance();
        return employeeBalanceRepository.save(balance);
    }

    /**
     * Get an employee's balance for a specific leave type.
     *
     * @param employeeId  the employee ID
     * @param leaveTypeId the leave type ID
     * @return the balance
     * @throws EntityNotFoundException if the balance is not found
     */
    @Transactional(readOnly = true)
    public EmployeeBalance getBalance(Long employeeId, Long leaveTypeId) {
        User employee = new User();
        employee.setId(employeeId);
        LeaveType leaveType = leaveTypeService.getLeaveType(leaveTypeId);

        return employeeBalanceRepository.findByEmployeeAndLeaveType(employee, leaveType)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Balance not found for employee " + employeeId + " and leave type " + leaveTypeId));
    }

    /**
     * Get all balances for an employee.
     *
     * @param employee the employee
     * @return list of balances
     */
    @Transactional(readOnly = true)
    public List<EmployeeBalance> getEmployeeBalances(User employee) {
        return employeeBalanceRepository.findByEmployee(employee);
    }

    /**
     * Update an employee's balance.
     *
     * @param balanceId  the balance ID
     * @param newBalance the new balance amount
     * @return the updated balance
     * @throws EntityNotFoundException if the balance is not found
     * @throws IllegalStateException   if the new balance is invalid
     */
    @Transactional
    public EmployeeBalance updateBalance(Long balanceId, BigDecimal newBalance) {
        log.info("Updating balance {} to {}", balanceId, newBalance);

        EmployeeBalance balance = employeeBalanceRepository.findById(balanceId)
                .orElseThrow(() -> new EntityNotFoundException("Balance not found with ID: " + balanceId));

        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Balance cannot be negative");
        }

        if (balance.getMaxBalance() != null && newBalance.compareTo(balance.getMaxBalance()) > 0) {
            throw new IllegalStateException("New balance exceeds maximum allowed balance");
        }

        balance.setCurrentBalance(newBalance);
        balance.validateBalance();
        return employeeBalanceRepository.save(balance);
    }

    /**
     * Adjust an employee's balance by a delta amount.
     *
     * @param balanceId the balance ID
     * @param delta     the amount to adjust by (positive for addition, negative for
     *                  subtraction)
     * @return the updated balance
     * @throws EntityNotFoundException if the balance is not found
     * @throws IllegalStateException   if the adjustment would result in an invalid
     *                                 balance
     */
    @Transactional
    public EmployeeBalance adjustBalance(Long balanceId, BigDecimal delta) {
        log.info("Adjusting balance {} by {}", balanceId, delta);

        EmployeeBalance balance = employeeBalanceRepository.findById(balanceId)
                .orElseThrow(() -> new EntityNotFoundException("Balance not found with ID: " + balanceId));

        BigDecimal newBalance = balance.getCurrentBalance().add(delta);
        return updateBalance(balanceId, newBalance);
    }

    /**
     * Update the maximum balance for an employee's leave type.
     *
     * @param balanceId     the balance ID
     * @param newMaxBalance the new maximum balance
     * @return the updated balance
     * @throws EntityNotFoundException if the balance is not found
     * @throws IllegalStateException   if the new maximum is invalid
     */
    @Transactional
    public EmployeeBalance updateMaxBalance(Long balanceId, BigDecimal newMaxBalance) {
        log.info("Updating maximum balance for balance {} to {}", balanceId, newMaxBalance);

        EmployeeBalance balance = employeeBalanceRepository.findById(balanceId)
                .orElseThrow(() -> new EntityNotFoundException("Balance not found with ID: " + balanceId));

        if (newMaxBalance != null && newMaxBalance.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Maximum balance must be greater than zero");
        }

        if (newMaxBalance != null && balance.getCurrentBalance().compareTo(newMaxBalance) > 0) {
            throw new IllegalStateException("Current balance exceeds new maximum balance");
        }

        balance.setMaxBalance(newMaxBalance);
        balance.validateBalance();
        return employeeBalanceRepository.save(balance);
    }

    /**
     * Update the accrual eligibility for an employee's balance.
     *
     * @param balanceId  the balance ID
     * @param isEligible whether the employee is eligible for accrual
     * @return the updated balance
     * @throws EntityNotFoundException if the balance is not found
     */
    @Transactional
    public EmployeeBalance updateAccrualEligibility(Long balanceId, boolean isEligible) {
        log.info("Updating accrual eligibility for balance {} to {}", balanceId, isEligible);

        EmployeeBalance balance = employeeBalanceRepository.findById(balanceId)
                .orElseThrow(() -> new EntityNotFoundException("Balance not found with ID: " + balanceId));

        balance.setIsEligibleForAccrual(isEligible);
        return employeeBalanceRepository.save(balance);
    }

    /**
     * Get all balances that are eligible for accrual processing.
     *
     * @param date the date to check against
     * @return list of eligible balances
     */
    @Transactional(readOnly = true)
    public List<EmployeeBalance> getEligibleForAccrual(LocalDate date) {
        return employeeBalanceRepository.findEligibleForAccrual(date);
    }

    /**
     * Get all balances that are eligible for carry-forward processing.
     *
     * @return list of eligible balances
     */
    @Transactional(readOnly = true)
    public List<EmployeeBalance> getEligibleForCarryForward() {
        return employeeBalanceRepository.findEligibleForCarryForward(BigDecimal.ZERO);
    }

    /**
     * Get all balances that exceed their maximum balance.
     *
     * @return list of balances exceeding maximum
     */
    @Transactional(readOnly = true)
    public List<EmployeeBalance> getExceedingMaxBalance() {
        return employeeBalanceRepository.findExceedingMaxBalance();
    }

    /**
     * Initialize balances for a new employee for all default leave types.
     *
     * @param employee the new employee
     * @return list of created balances
     */
    @Transactional
    public List<EmployeeBalance> initializeBalancesForNewEmployee(User employee) {
        log.info("Initializing balances for new employee {}", employee.getId());

        List<LeaveType> defaultLeaveTypes = leaveTypeService.getDefaultLeaveTypes();
        return defaultLeaveTypes.stream()
                .map(leaveType -> {
                    EmployeeBalance balance = new EmployeeBalance();
                    balance.setEmployee(employee);
                    balance.setLeaveType(leaveType);
                    balance.setCurrentBalance(BigDecimal.ZERO);
                    balance.setMaxBalance(
                            leaveType.getMaxDuration() != null ? new BigDecimal(leaveType.getMaxDuration()) : null);
                    balance.setIsEligibleForAccrual(
                            employee.getStatus() == User.Status.ACTIVE && leaveType.getAccrualBased());
                    balance.validateBalance();
                    return employeeBalanceRepository.save(balance);
                })
                .toList();
    }

    /**
     * Find all balances for a specific employee.
     *
     * @param employee the employee to find balances for
     * @return list of employee balances
     */
    public List<EmployeeBalance> findByEmployee(User employee) {
        return employeeBalanceRepository.findByEmployee(employee);
    }

    @Transactional
    public void processAccrual(User employee, LeaveType leaveType, LocalDate accrualDate) {
        log.info("Processing accrual for employee: {} and leave type: {}", employee.getId(), leaveType.getId());

        // Check if leave type is enabled
        if (!leaveType.getIsEnabled()) {
            log.info("Skipping accrual for disabled leave type: {}", leaveType.getName());
            return;
        }

        // Check if leave type is accrual-based
        if (!leaveType.getAccrualBased() || leaveType.getAccrualRate() == null) {
            log.info("Leave type {} is not accrual-based or has no accrual rate", leaveType.getName());
            return;
        }
    }
}
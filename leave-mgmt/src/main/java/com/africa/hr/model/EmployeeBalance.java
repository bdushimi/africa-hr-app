package com.africa.hr.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entity to track employee leave balances for each leave type.
 * This includes current balance, maximum balance, and accrual tracking.
 */
@Entity
@Table(name = "employee_balance")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Employee is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private User employee;

    @NotNull(message = "Leave type is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leave_type_id", nullable = false)
    private LeaveType leaveType;

    @NotNull(message = "Current balance is required")
    @Column(name = "current_balance", nullable = false, precision = 5, scale = 2)
    @DecimalMin(value = "0.00", message = "Current balance cannot be negative")
    @Digits(integer = 2, fraction = 2, message = "Current balance must have at most 2 decimal places")
    private BigDecimal currentBalance = BigDecimal.ZERO;

    @Column(name = "max_balance", precision = 5, scale = 2)
    @DecimalMin(value = "0.01", message = "Maximum balance must be greater than 0")
    @Digits(integer = 2, fraction = 2, message = "Maximum balance must have at most 2 decimal places")
    private BigDecimal maxBalance;

    @Column(name = "last_accrual_date")
    private LocalDate lastAccrualDate;

    @NotNull(message = "Eligibility for accrual flag is required")
    @Column(name = "is_eligible_for_accrual", nullable = false)
    private Boolean isEligibleForAccrual = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private java.time.LocalDateTime createdAt;

    @Column(name = "updated_at")
    private java.time.LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = java.time.LocalDateTime.now();
        validateBalance();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = java.time.LocalDateTime.now();
        validateBalance();
    }

    /**
     * Validates the employee balance configuration.
     * This method should be called before persisting or updating a balance.
     *
     * @throws IllegalStateException if the configuration is invalid
     */
    public void validateBalance() {
        // Validate current balance is not negative
        if (currentBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Current balance cannot be negative");
        }

        // Validate current balance does not exceed maximum balance
        if (maxBalance != null && currentBalance.compareTo(maxBalance) > 0) {
            throw new IllegalStateException("Current balance cannot exceed maximum balance");
        }

        // Validate last accrual date is not in the future
        if (lastAccrualDate != null && lastAccrualDate.isAfter(LocalDate.now())) {
            throw new IllegalStateException("Last accrual date cannot be in the future");
        }
    }

    /**
     * Checks if the employee is eligible for accrual based on their status and
     * leave type.
     *
     * @return true if the employee is eligible for accrual, false otherwise
     */
    public boolean isEligibleForAccrual() {
        return isEligibleForAccrual
                && employee.getStatus() == User.Status.ACTIVE
                && leaveType.getAccrualBased();
    }

    /**
     * Calculates the prorated accrual amount for an employee who joined mid-month.
     *
     * @param monthStart the start date of the month
     * @param monthEnd   the end date of the month
     * @return the prorated accrual amount
     */
    public BigDecimal calculateProratedAccrual(LocalDate monthStart, LocalDate monthEnd) {
        if (!isEligibleForAccrual() || leaveType.getAccrualRate() == null) {
            return BigDecimal.ZERO;
        }

        // If employee joined before the month, return full accrual
        if (employee.getJoinedDate().isBefore(monthStart)) {
            return leaveType.getAccrualRate();
        }

        // If employee joined after the month, return zero
        if (employee.getJoinedDate().isAfter(monthEnd)) {
            return BigDecimal.ZERO;
        }

        // Calculate prorated accrual for mid-month join
        long totalDays = monthEnd.toEpochDay() - monthStart.toEpochDay() + 1;
        long workedDays = monthEnd.toEpochDay() - employee.getJoinedDate().toEpochDay() + 1;

        return leaveType.getAccrualRate()
                .multiply(BigDecimal.valueOf(workedDays))
                .divide(BigDecimal.valueOf(totalDays), 2, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * Gets the total leave allowance for this balance.
     * For accrual-based leave types, this is based on the max duration.
     * For non-accrual leave types, this is based on either max duration or max
     * balance.
     *
     * @return the total allowance as a BigDecimal
     */
    public BigDecimal getTotalAllowance() {
        // For accrual-based leave types, use maxDuration if available
        if (leaveType.getAccrualBased() && leaveType.getMaxDuration() != null) {
            return new BigDecimal(leaveType.getMaxDuration());
        }

        // For non-accrual types, use maxDuration if available
        if (leaveType.getMaxDuration() != null) {
            return new BigDecimal(leaveType.getMaxDuration());
        }

        // Fall back to maxBalance if available
        return maxBalance != null ? maxBalance : BigDecimal.ZERO;
    }
}
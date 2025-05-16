package com.africa.hr.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

import com.africa.hr.validation.annotations.ValidCarryForwardAmounts;
import com.africa.hr.validation.annotations.ValidDateRange;

@ValidCarryForwardAmounts
public class LeaveCarryForwardDTO {
    @NotNull(message = "Employee balance ID is required")
    private Long employeeBalanceId;

    @NotNull(message = "From year is required")
    @Min(value = 2000, message = "From year must be at least 2000")
    private Integer fromYear;

    @NotNull(message = "To year is required")
    @Min(value = 2000, message = "To year must be at least 2000")
    private Integer toYear;

    @NotNull(message = "Original balance is required")
    @DecimalMin(value = "0.00", message = "Original balance must be non-negative")
    private BigDecimal originalBalance;

    @NotNull(message = "Carried forward amount is required")
    @DecimalMin(value = "0.00", message = "Carried forward amount must be non-negative")
    private BigDecimal carriedForwardAmount;

    @NotNull(message = "Forfeited amount is required")
    @DecimalMin(value = "0.00", message = "Forfeited amount must be non-negative")
    private BigDecimal forfeitedAmount;

    @NotNull(message = "Carry forward date is required")
    @ValidDateRange(message = "Carry forward date cannot be in the future")
    private LocalDate carryForwardDate;

    // Getters and setters
    public Long getEmployeeBalanceId() {
        return employeeBalanceId;
    }

    public void setEmployeeBalanceId(Long employeeBalanceId) {
        this.employeeBalanceId = employeeBalanceId;
    }

    public Integer getFromYear() {
        return fromYear;
    }

    public void setFromYear(Integer fromYear) {
        this.fromYear = fromYear;
    }

    public Integer getToYear() {
        return toYear;
    }

    public void setToYear(Integer toYear) {
        this.toYear = toYear;
    }

    public BigDecimal getOriginalBalance() {
        return originalBalance;
    }

    public void setOriginalBalance(BigDecimal originalBalance) {
        this.originalBalance = originalBalance;
    }

    public BigDecimal getCarriedForwardAmount() {
        return carriedForwardAmount;
    }

    public void setCarriedForwardAmount(BigDecimal carriedForwardAmount) {
        this.carriedForwardAmount = carriedForwardAmount;
    }

    public BigDecimal getForfeitedAmount() {
        return forfeitedAmount;
    }

    public void setForfeitedAmount(BigDecimal forfeitedAmount) {
        this.forfeitedAmount = forfeitedAmount;
    }

    public LocalDate getCarryForwardDate() {
        return carryForwardDate;
    }

    public void setCarryForwardDate(LocalDate carryForwardDate) {
        this.carryForwardDate = carryForwardDate;
    }
}
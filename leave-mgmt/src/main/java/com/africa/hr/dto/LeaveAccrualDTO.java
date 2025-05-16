package com.africa.hr.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

import com.africa.hr.validation.annotations.ValidDateRange;
import com.africa.hr.validation.annotations.ValidYearMonth;

public class LeaveAccrualDTO {
    @NotNull(message = "Employee balance ID is required")
    private Long employeeBalanceId;

    @NotNull(message = "Accrual date is required")
    @ValidDateRange(message = "Accrual date cannot be in the future")
    private LocalDate accrualDate;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotNull(message = "Accrual period is required")
    @ValidYearMonth(message = "Accrual period must be in format YYYY-MM")
    private String accrualPeriod;

    private boolean isProrated;

    // Getters and setters
    public Long getEmployeeBalanceId() {
        return employeeBalanceId;
    }

    public void setEmployeeBalanceId(Long employeeBalanceId) {
        this.employeeBalanceId = employeeBalanceId;
    }

    public LocalDate getAccrualDate() {
        return accrualDate;
    }

    public void setAccrualDate(LocalDate accrualDate) {
        this.accrualDate = accrualDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getAccrualPeriod() {
        return accrualPeriod;
    }

    public void setAccrualPeriod(String accrualPeriod) {
        this.accrualPeriod = accrualPeriod;
    }

    public boolean isProrated() {
        return isProrated;
    }

    public void setProrated(boolean prorated) {
        isProrated = prorated;
    }
}
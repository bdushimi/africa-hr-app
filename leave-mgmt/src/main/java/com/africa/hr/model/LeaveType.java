package com.africa.hr.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "leave_types")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaveType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name is required")
    @Column(unique = true, nullable = false)
    private String name;

    @Column(length = 500)
    private String description;

    @NotNull(message = "Default flag is required")
    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    @NotNull(message = "Enabled flag is required")
    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled = true;

    @Column(name = "max_duration")
    private Integer maxDuration;

    @NotNull(message = "Paid flag is required")
    @Column(nullable = false)
    private Boolean paid = true;

    // Accrual configuration
    @NotNull(message = "Accrual based flag is required")
    @Column(name = "accrual_based", nullable = false)
    private Boolean accrualBased = false;

    @Column(name = "accrual_rate", precision = 5, scale = 2)
    @DecimalMin(value = "0.01", message = "Accrual rate must be greater than 0")
    @DecimalMax(value = "31.00", message = "Accrual rate cannot exceed 31.00")
    @Digits(integer = 2, fraction = 2, message = "Accrual rate must have at most 2 decimal places")
    private BigDecimal accrualRate;

    // Carry-forward configuration
    @NotNull(message = "Carry-forward enabled flag is required")
    @Column(name = "is_carry_forward_enabled", nullable = false)
    private Boolean isCarryForwardEnabled = false;

    @Column(name = "carry_forward_cap", precision = 5, scale = 2)
    @DecimalMin(value = "0.01", message = "Carry-forward cap must be greater than 0")
    @Digits(integer = 2, fraction = 2, message = "Carry-forward cap must have at most 2 decimal places")
    private BigDecimal carryForwardCap;

    // Validation configuration
    @NotNull(message = "Require reason flag is required")
    @Column(name = "require_reason", nullable = false)
    private Boolean requireReason = false;

    @NotNull(message = "Require document flag is required")
    @Column(name = "require_document", nullable = false)
    private Boolean requireDocument = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private java.time.LocalDateTime createdAt;

    @Column(name = "updated_at")
    private java.time.LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = java.time.LocalDateTime.now();
        updatedAt = java.time.LocalDateTime.now();
        validateConfiguration();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = java.time.LocalDateTime.now();
        validateConfiguration();
    }

    /**
     * Validates the leave type configuration.
     * This method should be called before persisting or updating a leave type.
     *
     * @throws IllegalStateException if the configuration is invalid
     */
    public void validateConfiguration() {
        // Validate accrual rate
        if (accrualBased && (accrualRate == null || accrualRate.compareTo(BigDecimal.ZERO) <= 0)) {
            throw new IllegalStateException("Accrual-based leave types must have a positive accrual rate");
        }
        if (!accrualBased && accrualRate != null) {
            throw new IllegalStateException("Non-accrual leave types cannot have an accrual rate");
        }

        // Validate carry-forward cap
        if (isCarryForwardEnabled && (carryForwardCap == null || carryForwardCap.compareTo(BigDecimal.ZERO) <= 0)) {
            throw new IllegalStateException(
                    "Leave types with carry-forward enabled must have a positive carry-forward cap");
        }
        if (!isCarryForwardEnabled && carryForwardCap != null) {
            throw new IllegalStateException("Leave types without carry-forward cannot have a carry-forward cap");
        }

        // Validate max balance against annual accrual
        if (maxDuration != null && accrualBased && accrualRate != null) {
            BigDecimal annualAccrual = accrualRate.multiply(new BigDecimal("12"));
            if (annualAccrual.compareTo(new BigDecimal(maxDuration)) > 0) {
                throw new IllegalStateException("Annual accrual cannot exceed maximum duration");
            }
        }

        // Validate that default leave types are always enabled
        if (isDefault && !isEnabled) {
            throw new IllegalStateException("Default leave types cannot be disabled");
        }
    }
}
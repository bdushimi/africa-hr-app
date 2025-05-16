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
 * Entity to track leave carry-forward history.
 * Records each carry-forward transaction with its amount and date.
 */
@Entity
@Table(name = "leave_carry_forwards")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaveCarryForward {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Employee balance is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_balance_id", nullable = false)
    private EmployeeBalance employeeBalance;

    @NotNull(message = "From year is required")
    @Column(name = "from_year", nullable = false)
    private Integer fromYear;

    @NotNull(message = "To year is required")
    @Column(name = "to_year", nullable = false)
    private Integer toYear;

    @NotNull(message = "Carry-forward date is required")
    @Column(name = "carry_forward_date", nullable = false)
    private LocalDate carryForwardDate;

    @NotNull(message = "Original balance is required")
    @Column(name = "original_balance", nullable = false, precision = 5, scale = 2)
    @DecimalMin(value = "0.00", message = "Original balance cannot be negative")
    @Digits(integer = 2, fraction = 2, message = "Original balance must have at most 2 decimal places")
    private BigDecimal originalBalance;

    @NotNull(message = "Carried forward amount is required")
    @Column(name = "carried_forward_amount", nullable = false, precision = 5, scale = 2)
    @DecimalMin(value = "0.00", message = "Carried forward amount cannot be negative")
    @Digits(integer = 2, fraction = 2, message = "Carried forward amount must have at most 2 decimal places")
    private BigDecimal carriedForwardAmount;

    @NotNull(message = "Forfeited amount is required")
    @Column(name = "forfeited_amount", nullable = false, precision = 5, scale = 2)
    @DecimalMin(value = "0.00", message = "Forfeited amount cannot be negative")
    @Digits(integer = 2, fraction = 2, message = "Forfeited amount must have at most 2 decimal places")
    private BigDecimal forfeitedAmount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private java.time.LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = java.time.LocalDateTime.now();
        validateAmounts();
    }

    /**
     * Validates that the amounts add up correctly.
     *
     * @throws IllegalStateException if the amounts are invalid
     */
    @PreUpdate
    public void validateAmounts() {
        if (carriedForwardAmount.add(forfeitedAmount).compareTo(originalBalance) != 0) {
            throw new IllegalStateException(
                    "Carried forward amount plus forfeited amount must equal original balance");
        }
    }
}
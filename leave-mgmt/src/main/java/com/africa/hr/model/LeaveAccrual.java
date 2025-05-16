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
import java.time.YearMonth;

/**
 * Entity to track leave accrual history.
 * Records each accrual transaction with its amount and date.
 */
@Entity
@Table(name = "leave_accruals")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaveAccrual {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Employee balance is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_balance_id", nullable = false)
    private EmployeeBalance employeeBalance;

    @NotNull(message = "Accrual date is required")
    @Column(name = "accrual_date", nullable = false)
    private LocalDate accrualDate;

    @NotNull(message = "Accrual period is required")
    @Column(name = "accrual_period", nullable = false)
    @Convert(converter = YearMonthConverter.class)
    private YearMonth yearMonth;

    @NotNull(message = "Amount is required")
    @Column(nullable = false, precision = 5, scale = 2)
    @DecimalMin(value = "0.00", message = "Amount cannot be negative")
    @Digits(integer = 2, fraction = 2, message = "Amount must have at most 2 decimal places")
    private BigDecimal amount;

    @NotNull(message = "Prorated flag is required")
    @Column(name = "is_prorated", nullable = false)
    private Boolean isProrated = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private java.time.LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = java.time.LocalDateTime.now();
    }

    @Converter(autoApply = true)
    public static class YearMonthConverter implements AttributeConverter<YearMonth, LocalDate> {
        @Override
        public LocalDate convertToDatabaseColumn(YearMonth yearMonth) {
            return yearMonth != null ? yearMonth.atDay(1) : null;
        }

        @Override
        public YearMonth convertToEntityAttribute(LocalDate date) {
            return date != null ? YearMonth.from(date) : null;
        }
    }
}
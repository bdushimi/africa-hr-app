package com.africa.hr.validation.validators;

import com.africa.hr.validation.annotations.ValidEmployeeBalance;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import java.math.BigDecimal;
import java.time.LocalDate;

public class EmployeeBalanceValidator implements ConstraintValidator<ValidEmployeeBalance, Object> {
    private String currentBalanceField;
    private String maxBalanceField;
    private String lastAccrualDateField;
    private String isEligibleForAccrualField;

    @Override
    public void initialize(ValidEmployeeBalance constraintAnnotation) {
        this.currentBalanceField = constraintAnnotation.currentBalanceField();
        this.maxBalanceField = constraintAnnotation.maxBalanceField();
        this.lastAccrualDateField = constraintAnnotation.lastAccrualDateField();
        this.isEligibleForAccrualField = constraintAnnotation.isEligibleForAccrualField();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(value);

        BigDecimal currentBalance = (BigDecimal) wrapper.getPropertyValue(currentBalanceField);
        BigDecimal maxBalance = (BigDecimal) wrapper.getPropertyValue(maxBalanceField);
        LocalDate lastAccrualDate = (LocalDate) wrapper.getPropertyValue(lastAccrualDateField);
        Boolean isEligibleForAccrual = (Boolean) wrapper.getPropertyValue(isEligibleForAccrualField);

        // Validate current balance
        if (currentBalance != null && currentBalance.compareTo(BigDecimal.ZERO) < 0) {
            addConstraintViolation(context, "Current balance cannot be negative");
            return false;
        }

        // Validate max balance
        if (maxBalance != null) {
            if (maxBalance.compareTo(BigDecimal.ZERO) <= 0) {
                addConstraintViolation(context, "Maximum balance must be greater than 0");
                return false;
            }
            if (currentBalance != null && currentBalance.compareTo(maxBalance) > 0) {
                addConstraintViolation(context, "Current balance cannot exceed maximum balance");
                return false;
            }
        }

        // Validate last accrual date
        if (lastAccrualDate != null && lastAccrualDate.isAfter(LocalDate.now())) {
            addConstraintViolation(context, "Last accrual date cannot be in the future");
            return false;
        }

        // Validate accrual eligibility
        if (isEligibleForAccrual != null && !isEligibleForAccrual && lastAccrualDate == null) {
            addConstraintViolation(context, "Last accrual date is required when employee is not eligible for accrual");
            return false;
        }

        return true;
    }

    private void addConstraintViolation(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
                .addConstraintViolation();
    }
}
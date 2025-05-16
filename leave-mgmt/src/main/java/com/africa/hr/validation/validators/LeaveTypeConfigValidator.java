package com.africa.hr.validation.validators;

import com.africa.hr.validation.annotations.ValidLeaveTypeConfig;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import java.math.BigDecimal;

public class LeaveTypeConfigValidator implements ConstraintValidator<ValidLeaveTypeConfig, Object> {
    private String accrualBasedField;
    private String accrualRateField;
    private String isCarryForwardEnabledField;
    private String carryForwardCapField;
    private String maxDurationField;

    @Override
    public void initialize(ValidLeaveTypeConfig constraintAnnotation) {
        this.accrualBasedField = constraintAnnotation.accrualBasedField();
        this.accrualRateField = constraintAnnotation.accrualRateField();
        this.isCarryForwardEnabledField = constraintAnnotation.isCarryForwardEnabledField();
        this.carryForwardCapField = constraintAnnotation.carryForwardCapField();
        this.maxDurationField = constraintAnnotation.maxDurationField();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(value);

        Boolean accrualBased = (Boolean) wrapper.getPropertyValue(accrualBasedField);
        BigDecimal accrualRate = (BigDecimal) wrapper.getPropertyValue(accrualRateField);
        Boolean isCarryForwardEnabled = (Boolean) wrapper.getPropertyValue(isCarryForwardEnabledField);
        BigDecimal carryForwardCap = (BigDecimal) wrapper.getPropertyValue(carryForwardCapField);
        Integer maxDuration = (Integer) wrapper.getPropertyValue(maxDurationField);

        // Validate accrual configuration
        if (accrualBased != null && accrualBased) {
            if (accrualRate == null || accrualRate.compareTo(BigDecimal.ZERO) <= 0 ||
                    accrualRate.compareTo(new BigDecimal("31.00")) > 0) {
                addConstraintViolation(context,
                        "Accrual rate must be between 0.01 and 31.00 when accrual based is true");
                return false;
            }
        } else if (accrualRate != null) {
            addConstraintViolation(context, "Accrual rate must be null when accrual based is false");
            return false;
        }

        // Validate carry forward configuration
        if (isCarryForwardEnabled != null && isCarryForwardEnabled) {
            if (carryForwardCap == null || carryForwardCap.compareTo(BigDecimal.ZERO) <= 0) {
                addConstraintViolation(context,
                        "Carry forward cap must be greater than 0 when carry forward is enabled");
                return false;
            }
        } else if (carryForwardCap != null) {
            addConstraintViolation(context, "Carry forward cap must be null when carry forward is disabled");
            return false;
        }

        // Validate max duration
        if (maxDuration != null && maxDuration <= 0) {
            addConstraintViolation(context, "Maximum duration must be greater than 0");
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
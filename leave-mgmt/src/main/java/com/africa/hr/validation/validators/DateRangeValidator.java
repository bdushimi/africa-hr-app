package com.africa.hr.validation.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;

import com.africa.hr.validation.annotations.ValidDateRange;

public class DateRangeValidator implements ConstraintValidator<ValidDateRange, LocalDate> {
    private boolean allowFuture;

    @Override
    public void initialize(ValidDateRange constraintAnnotation) {
        this.allowFuture = constraintAnnotation.allowFuture();
    }

    @Override
    public boolean isValid(LocalDate value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Let @NotNull handle null validation
        }

        if (!allowFuture && value.isAfter(LocalDate.now())) {
            return false;
        }

        return true;
    }
}
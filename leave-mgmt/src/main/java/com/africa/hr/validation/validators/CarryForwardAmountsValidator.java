package com.africa.hr.validation.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;

import com.africa.hr.validation.annotations.ValidCarryForwardAmounts;

import java.math.BigDecimal;

public class CarryForwardAmountsValidator implements ConstraintValidator<ValidCarryForwardAmounts, Object> {
    private String originalBalanceField;
    private String carriedForwardField;
    private String forfeitedField;

    @Override
    public void initialize(ValidCarryForwardAmounts constraintAnnotation) {
        this.originalBalanceField = constraintAnnotation.originalBalanceField();
        this.carriedForwardField = constraintAnnotation.carriedForwardField();
        this.forfeitedField = constraintAnnotation.forfeitedField();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(value);

        BigDecimal originalBalance = (BigDecimal) wrapper.getPropertyValue(originalBalanceField);
        BigDecimal carriedForward = (BigDecimal) wrapper.getPropertyValue(carriedForwardField);
        BigDecimal forfeited = (BigDecimal) wrapper.getPropertyValue(forfeitedField);

        if (originalBalance == null || carriedForward == null || forfeited == null) {
            return true; // Let @NotNull handle null validation
        }

        // Check if amounts are non-negative
        if (originalBalance.compareTo(BigDecimal.ZERO) < 0 ||
                carriedForward.compareTo(BigDecimal.ZERO) < 0 ||
                forfeited.compareTo(BigDecimal.ZERO) < 0) {
            return false;
        }

        // Check if carried forward + forfeited equals original balance
        return carriedForward.add(forfeited).compareTo(originalBalance) == 0;
    }
}
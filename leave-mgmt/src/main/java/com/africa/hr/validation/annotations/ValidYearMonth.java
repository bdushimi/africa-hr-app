package com.africa.hr.validation.annotations;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

import com.africa.hr.validation.validators.YearMonthValidator;

@Documented
@Constraint(validatedBy = YearMonthValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidYearMonth {
    String message() default "Year-month must be in format YYYY-MM";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
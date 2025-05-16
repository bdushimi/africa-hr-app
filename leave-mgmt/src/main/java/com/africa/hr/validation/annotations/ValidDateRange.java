package com.africa.hr.validation.annotations;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

import com.africa.hr.validation.validators.DateRangeValidator;

@Documented
@Constraint(validatedBy = DateRangeValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidDateRange {
    String message() default "Date must not be in the future";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    boolean allowFuture() default false;
}
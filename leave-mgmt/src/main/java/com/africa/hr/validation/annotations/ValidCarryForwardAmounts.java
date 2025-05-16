package com.africa.hr.validation.annotations;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

import com.africa.hr.validation.validators.CarryForwardAmountsValidator;

@Documented
@Constraint(validatedBy = CarryForwardAmountsValidator.class)
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCarryForwardAmounts {
    String message() default "Carry forward amounts must be valid";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    String originalBalanceField() default "originalBalance";

    String carriedForwardField() default "carriedForwardAmount";

    String forfeitedField() default "forfeitedAmount";
}
package com.africa.hr.validation.annotations;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

import com.africa.hr.validation.validators.EmployeeBalanceValidator;

@Documented
@Constraint(validatedBy = EmployeeBalanceValidator.class)
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidEmployeeBalance {
    String message() default "Employee balance configuration is invalid";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    String currentBalanceField() default "currentBalance";

    String maxBalanceField() default "maxBalance";

    String lastAccrualDateField() default "lastAccrualDate";

    String isEligibleForAccrualField() default "isEligibleForAccrual";

    String leaveTypeField() default "leaveType";
}

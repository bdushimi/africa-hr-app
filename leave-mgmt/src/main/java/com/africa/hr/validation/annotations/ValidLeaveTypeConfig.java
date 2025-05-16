package com.africa.hr.validation.annotations;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

import com.africa.hr.validation.validators.LeaveTypeConfigValidator;

@Documented
@Constraint(validatedBy = LeaveTypeConfigValidator.class)
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidLeaveTypeConfig {
    String message() default "Leave type configuration is invalid";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    String accrualBasedField() default "accrualBased";

    String accrualRateField() default "accrualRate";

    String isCarryForwardEnabledField() default "isCarryForwardEnabled";

    String carryForwardCapField() default "carryForwardCap";

    String maxDurationField() default "maxDuration";
}

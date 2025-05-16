package com.africa.hr.validation.annotations;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

import com.africa.hr.validation.validators.LeaveRequestDatesValidator;

@Documented
@Constraint(validatedBy = LeaveRequestDatesValidator.class)
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidLeaveRequestDates {
    String message() default "Leave request dates are invalid";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    String startDateField() default "startDate";

    String endDateField() default "endDate";

    String halfDayStartField() default "halfDayStart";

    String halfDayEndField() default "halfDayEnd";

    String leaveTypeIdField() default "leaveTypeId";
}
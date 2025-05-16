package com.africa.hr.validation.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.africa.hr.service.LeaveTypeService;
import com.africa.hr.validation.annotations.ValidLeaveRequestDates;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class LeaveRequestDatesValidator implements ConstraintValidator<ValidLeaveRequestDates, Object> {
    private String startDateField;
    private String endDateField;
    private String halfDayStartField;
    private String halfDayEndField;
    private String leaveTypeIdField;

    @Autowired
    private LeaveTypeService leaveTypeService;

    @Override
    public void initialize(ValidLeaveRequestDates constraintAnnotation) {
        this.startDateField = constraintAnnotation.startDateField();
        this.endDateField = constraintAnnotation.endDateField();
        this.halfDayStartField = constraintAnnotation.halfDayStartField();
        this.halfDayEndField = constraintAnnotation.halfDayEndField();
        this.leaveTypeIdField = constraintAnnotation.leaveTypeIdField();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(value);

        LocalDate startDate = (LocalDate) wrapper.getPropertyValue(startDateField);
        LocalDate endDate = (LocalDate) wrapper.getPropertyValue(endDateField);
        Boolean halfDayStart = (Boolean) wrapper.getPropertyValue(halfDayStartField);
        Boolean halfDayEnd = (Boolean) wrapper.getPropertyValue(halfDayEndField);
        Long leaveTypeId = (Long) wrapper.getPropertyValue(leaveTypeIdField);

        // Validate dates are not null
        if (startDate == null || endDate == null) {
            addConstraintViolation(context, "Start date and end date are required");
            return false;
        }

        // Validate dates are not in the past
        LocalDate today = LocalDate.now();
        if (startDate.isBefore(today)) {
            addConstraintViolation(context, "Start date cannot be in the past. Please select a future date.");
            return false;
        }

        // Validate end date is not before start date
        if (endDate.isBefore(startDate)) {
            addConstraintViolation(context, "End date cannot be before start date");
            return false;
        }

        // Calculate duration in days
        long durationInDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;

        // Adjust for half days
        if (Boolean.TRUE.equals(halfDayStart)) {
            durationInDays -= 0.5;
        }
        if (Boolean.TRUE.equals(halfDayEnd)) {
            durationInDays -= 0.5;
        }

        // Validate duration against leave type's max duration
        if (leaveTypeId != null) {
            var leaveType = leaveTypeService.getLeaveType(leaveTypeId);
            if (leaveType.getMaxDuration() != null && durationInDays > leaveType.getMaxDuration()) {
                addConstraintViolation(context,
                        String.format(
                                "Leave duration (%s days) exceeds maximum allowed duration (%s days) for leave type %s",
                                durationInDays, leaveType.getMaxDuration(), leaveType.getName()));
                return false;
            }
        }

        // Validate half-day configuration
        if (Boolean.TRUE.equals(halfDayStart) && Boolean.TRUE.equals(halfDayEnd) && startDate.equals(endDate)) {
            addConstraintViolation(context, "Cannot have both start and end as half days for a single day leave");
            return false;
        }

        // Validate minimum leave duration
        if (durationInDays < 1) {
            addConstraintViolation(context, "Leave duration must be at least 1 day");
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
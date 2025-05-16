package com.africa.hr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO containing both employee leave records and public holidays.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyCalendarDTO {
    private List<EmployeeLeaveDTO> employeeLeaves;
    private List<PublicHolidayDTO> publicHolidays;
    private List<DepartmentDTO> departments;
}
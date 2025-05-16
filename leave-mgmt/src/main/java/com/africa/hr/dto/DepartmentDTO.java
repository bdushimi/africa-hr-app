package com.africa.hr.dto;

import com.africa.hr.model.Department;
import lombok.Data;

/**
 * DTO for Department information
 */
@Data
public class DepartmentDTO {
    private Long id;
    private String name;
    private String description;

    /**
     * Convert Department entity to DTO
     * 
     * @param department the department entity
     * @return the department DTO
     */
    public static DepartmentDTO fromEntity(Department department) {
        if (department == null)
            return null;
        DepartmentDTO dto = new DepartmentDTO();
        dto.setId(department.getId());
        dto.setName(department.getName());
        dto.setDescription(department.getDescription());
        return dto;
    }
}
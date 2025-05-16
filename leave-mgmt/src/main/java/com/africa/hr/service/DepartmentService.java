package com.africa.hr.service;

import com.africa.hr.model.Department;
import com.africa.hr.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    /**
     * Get all departments
     *
     * @return list of all departments
     */
    @Transactional(readOnly = true)
    public List<Department> getAllDepartments() {
        log.info("Fetching all departments");
        return departmentRepository.findAll();
    }
}
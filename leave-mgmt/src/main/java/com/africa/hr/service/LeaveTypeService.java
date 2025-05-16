package com.africa.hr.service;

import com.africa.hr.model.LeaveType;
import com.africa.hr.repository.LeaveTypeRepository;
import com.africa.hr.dto.LeaveTypeDTO;
import com.africa.hr.dto.LeaveTypeUpdateDTO;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for managing leave types in the system.
 * Handles CRUD operations and business logic for leave types.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveTypeService {

    private final LeaveTypeRepository leaveTypeRepository;

    /**
     * Create a new leave type.
     *
     * @param leaveType the leave type to create
     * @return the created leave type
     * @throws IllegalStateException if a leave type with the same name exists
     */
    @Transactional
    public LeaveType createLeaveType(LeaveType leaveType) {
        log.info("Creating new leave type: {}", leaveType.getName());

        if (leaveTypeRepository.existsByName(leaveType.getName())) {
            throw new IllegalStateException("Leave type with name '" + leaveType.getName() + "' already exists");
        }

        leaveType.validateConfiguration();
        return leaveTypeRepository.save(leaveType);
    }

    /**
     * Update an existing leave type.
     *
     * @param id        the ID of the leave type to update
     * @param leaveType the updated leave type data
     * @return the updated leave type
     * @throws EntityNotFoundException if the leave type is not found
     * @throws IllegalStateException   if the update would violate business rules
     */
    @Transactional
    public LeaveType updateLeaveType(Long id, LeaveType leaveType) {
        log.info("Updating leave type with ID: {}", id);

        LeaveType existingLeaveType = leaveTypeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Leave type not found with ID: " + id));

        // Check if name is being changed and if new name already exists
        if (!existingLeaveType.getName().equals(leaveType.getName())
                && leaveTypeRepository.existsByName(leaveType.getName())) {
            throw new IllegalStateException("Leave type with name '" + leaveType.getName() + "' already exists");
        }

        // Validate that default leave types cannot be disabled
        if (existingLeaveType.getIsDefault() && !leaveType.getIsEnabled()) {
            throw new IllegalStateException("Default leave types cannot be disabled");
        }

        // Update fields
        existingLeaveType.setName(leaveType.getName());
        existingLeaveType.setDescription(leaveType.getDescription());
        existingLeaveType.setIsDefault(leaveType.getIsDefault());
        existingLeaveType.setIsEnabled(leaveType.getIsEnabled());
        existingLeaveType.setMaxDuration(leaveType.getMaxDuration());
        existingLeaveType.setPaid(leaveType.getPaid());
        existingLeaveType.setAccrualBased(leaveType.getAccrualBased());
        existingLeaveType.setAccrualRate(leaveType.getAccrualRate());
        existingLeaveType.setIsCarryForwardEnabled(leaveType.getIsCarryForwardEnabled());
        existingLeaveType.setCarryForwardCap(leaveType.getCarryForwardCap());
        existingLeaveType.setRequireReason(leaveType.getRequireReason());
        existingLeaveType.setRequireDocument(leaveType.getRequireDocument());

        existingLeaveType.validateConfiguration();
        return leaveTypeRepository.save(existingLeaveType);
    }

    /**
     * Get a leave type by ID.
     *
     * @param id the ID of the leave type
     * @return the leave type
     * @throws EntityNotFoundException if the leave type is not found
     */
    @Transactional(readOnly = true)
    public LeaveType getLeaveType(Long id) {
        return leaveTypeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Leave type not found with ID: " + id));
    }

    /**
     * Get a leave type by name.
     *
     * @param name the name of the leave type
     * @return the leave type
     * @throws EntityNotFoundException if the leave type is not found
     */
    @Transactional(readOnly = true)
    public LeaveType getLeaveTypeByName(String name) {
        return leaveTypeRepository.findByName(name)
                .orElseThrow(() -> new EntityNotFoundException("Leave type not found with name: " + name));
    }

    /**
     * Get all leave types.
     *
     * @return list of all leave types
     */
    @Transactional(readOnly = true)
    public List<LeaveType> getAllLeaveTypes() {
        return leaveTypeRepository.findAll();
    }

    /**
     * Get all default leave types.
     *
     * @return list of default leave types
     */
    @Transactional(readOnly = true)
    public List<LeaveType> getDefaultLeaveTypes() {
        return leaveTypeRepository.findByIsDefaultTrue();
    }

    /**
     * Get all accrual-based leave types.
     *
     * @return list of accrual-based leave types
     */
    @Transactional(readOnly = true)
    public List<LeaveType> getAccrualBasedLeaveTypes() {
        return leaveTypeRepository.findByAccrualBasedTrue();
    }

    /**
     * Get all leave types eligible for carry-forward.
     *
     * @return list of leave types eligible for carry-forward
     */
    @Transactional(readOnly = true)
    public List<LeaveType> getCarryForwardEligibleLeaveTypes() {
        return leaveTypeRepository.findEligibleForCarryForward();
    }

    /**
     * Delete a leave type.
     *
     * @param id the ID of the leave type to delete
     * @throws EntityNotFoundException if the leave type is not found
     * @throws IllegalStateException   if the leave type is in use
     */
    @Transactional
    public void deleteLeaveType(Long id) {
        log.info("Deleting leave type with ID: {}", id);

        LeaveType leaveType = leaveTypeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Leave type not found with ID: " + id));

        // TODO: Add check if leave type is in use (has associated balances or leave
        // requests)
        // This will be implemented when we have the EmployeeBalanceService

        leaveTypeRepository.delete(leaveType);
    }

    /**
     * Check if a leave type exists by name.
     *
     * @param name the name to check
     * @return true if the leave type exists
     */
    @Transactional(readOnly = true)
    public boolean existsByName(String name) {
        return leaveTypeRepository.existsByName(name);
    }

    /**
     * Create a new leave type from DTO.
     *
     * @param leaveTypeDTO the leave type DTO to create
     * @return the created leave type DTO
     * @throws IllegalStateException if a leave type with the same name exists
     */
    @Transactional
    public LeaveTypeDTO createLeaveType(LeaveTypeDTO leaveTypeDTO) {
        log.info("Creating new leave type from DTO: {}", leaveTypeDTO.getName());

        if (leaveTypeRepository.existsByName(leaveTypeDTO.getName())) {
            throw new IllegalStateException("Leave type with name '" + leaveTypeDTO.getName() + "' already exists");
        }

        // Validate that default leave types cannot be disabled
        if (Boolean.TRUE.equals(leaveTypeDTO.getIsDefault()) && !Boolean.TRUE.equals(leaveTypeDTO.getIsEnabled())) {
            throw new IllegalStateException("Default leave types cannot be disabled");
        }

        LeaveType leaveType = new LeaveType();
        leaveType.setName(leaveTypeDTO.getName());
        leaveType.setDescription(leaveTypeDTO.getDescription());
        leaveType.setIsDefault(leaveTypeDTO.getIsDefault());
        leaveType.setIsEnabled(leaveTypeDTO.getIsEnabled());
        leaveType.setMaxDuration(leaveTypeDTO.getMaxDuration());
        leaveType.setPaid(leaveTypeDTO.getPaid());
        leaveType.setAccrualBased(leaveTypeDTO.getAccrualBased());
        leaveType.setAccrualRate(leaveTypeDTO.getAccrualRate());
        leaveType.setIsCarryForwardEnabled(leaveTypeDTO.getIsCarryForwardEnabled());
        leaveType.setCarryForwardCap(leaveTypeDTO.getCarryForwardCap());
        leaveType.setRequireReason(leaveTypeDTO.getRequireReason());
        leaveType.setRequireDocument(leaveTypeDTO.getRequireDocument());

        leaveType.validateConfiguration();
        LeaveType savedLeaveType = leaveTypeRepository.save(leaveType);
        return convertToDTO(savedLeaveType);
    }

    /**
     * Get a leave type by ID and convert to DTO.
     *
     * @param id the ID of the leave type
     * @return the leave type DTO
     * @throws EntityNotFoundException if the leave type is not found
     */
    @Transactional(readOnly = true)
    public LeaveTypeDTO getLeaveTypeById(Long id) {
        LeaveType leaveType = getLeaveType(id);
        return convertToDTO(leaveType);
    }

    /**
     * Get all leave types and convert to DTOs.
     *
     * @return list of all leave type DTOs
     */
    @Transactional(readOnly = true)
    public List<LeaveTypeDTO> getAllLeaveTypesDTO() {
        return leaveTypeRepository.findAll().stream()
                .map(this::convertToDTO)
                .toList();
    }

    /**
     * Convert a LeaveType entity to a LeaveTypeDTO.
     *
     * @param leaveType the leave type entity
     * @return the leave type DTO
     */
    private LeaveTypeDTO convertToDTO(LeaveType leaveType) {
        LeaveTypeDTO dto = new LeaveTypeDTO();
        dto.setId(leaveType.getId());
        dto.setName(leaveType.getName());
        dto.setDescription(leaveType.getDescription());
        dto.setIsDefault(leaveType.getIsDefault());
        dto.setIsEnabled(leaveType.getIsEnabled());
        dto.setMaxDuration(leaveType.getMaxDuration());
        dto.setPaid(leaveType.getPaid());
        dto.setAccrualBased(leaveType.getAccrualBased());
        dto.setAccrualRate(leaveType.getAccrualRate());
        dto.setIsCarryForwardEnabled(leaveType.getIsCarryForwardEnabled());
        dto.setCarryForwardCap(leaveType.getCarryForwardCap());
        dto.setRequireReason(leaveType.getRequireReason());
        dto.setRequireDocument(leaveType.getRequireDocument());
        return dto;
    }

    /**
     * Update specific fields of a leave type.
     *
     * @param id        the ID of the leave type to update
     * @param updateDTO the DTO containing the fields to update
     * @return the updated leave type DTO
     * @throws EntityNotFoundException if the leave type is not found
     * @throws IllegalStateException   if the update would violate business rules
     */
    @Transactional
    public LeaveTypeDTO updateLeaveType(Long id, LeaveTypeUpdateDTO updateDTO) {
        log.info("Updating leave type with ID: {} using DTO", id);

        LeaveType existingLeaveType = leaveTypeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Leave type not found with ID: " + id));

        // Check if name is being changed and if new name already exists
        if (updateDTO.getName() != null && !existingLeaveType.getName().equals(updateDTO.getName())
                && leaveTypeRepository.existsByName(updateDTO.getName())) {
            throw new IllegalStateException("Leave type with name '" + updateDTO.getName() + "' already exists");
        }

        // Validate that default leave types cannot be disabled
        if (Boolean.TRUE.equals(existingLeaveType.getIsDefault()) &&
                Boolean.FALSE.equals(updateDTO.getIsEnabled())) {
            throw new IllegalStateException("Default leave types cannot be disabled");
        }

        // Update fields if not null
        if (updateDTO.getName() != null) {
            existingLeaveType.setName(updateDTO.getName());
        }
        if (updateDTO.getDescription() != null) {
            existingLeaveType.setDescription(updateDTO.getDescription());
        }
        if (updateDTO.getIsDefault() != null) {
            existingLeaveType.setIsDefault(updateDTO.getIsDefault());
        }
        if (updateDTO.getIsEnabled() != null) {
            existingLeaveType.setIsEnabled(updateDTO.getIsEnabled());
        }
        if (updateDTO.getMaxDuration() != null) {
            existingLeaveType.setMaxDuration(updateDTO.getMaxDuration());
        }
        if (updateDTO.getPaid() != null) {
            existingLeaveType.setPaid(updateDTO.getPaid());
        }
        if (updateDTO.getAccrualBased() != null) {
            existingLeaveType.setAccrualBased(updateDTO.getAccrualBased());
        }
        if (updateDTO.getAccrualRate() != null) {
            existingLeaveType.setAccrualRate(updateDTO.getAccrualRate());
        }
        if (updateDTO.getIsCarryForwardEnabled() != null) {
            existingLeaveType.setIsCarryForwardEnabled(updateDTO.getIsCarryForwardEnabled());
        }
        if (updateDTO.getCarryForwardCap() != null) {
            existingLeaveType.setCarryForwardCap(updateDTO.getCarryForwardCap());
        }
        if (updateDTO.getRequireReason() != null) {
            existingLeaveType.setRequireReason(updateDTO.getRequireReason());
        }
        if (updateDTO.getRequireDocument() != null) {
            existingLeaveType.setRequireDocument(updateDTO.getRequireDocument());
        }

        existingLeaveType.validateConfiguration();
        LeaveType updatedLeaveType = leaveTypeRepository.save(existingLeaveType);
        return convertToDTO(updatedLeaveType);
    }
}
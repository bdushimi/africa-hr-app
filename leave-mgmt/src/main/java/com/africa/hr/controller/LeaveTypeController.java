package com.africa.hr.controller;

import com.africa.hr.dto.LeaveTypeDTO;
import com.africa.hr.dto.LeaveTypeUpdateDTO;
import com.africa.hr.service.LeaveTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/leaveTypes")
@Tag(name = "Leave Types", description = "APIs for managing leave types")
public class LeaveTypeController {

    private final LeaveTypeService leaveTypeService;

    public LeaveTypeController(LeaveTypeService leaveTypeService) {
        this.leaveTypeService = leaveTypeService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @Operation(summary = "Get all leave types")
    public ResponseEntity<List<LeaveTypeDTO>> getAllLeaveTypes() {
        return ResponseEntity.ok(leaveTypeService.getAllLeaveTypesDTO());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new leave type")
    public ResponseEntity<LeaveTypeDTO> createLeaveType(@Valid @RequestBody LeaveTypeDTO leaveTypeDTO) {
        return ResponseEntity.ok(leaveTypeService.createLeaveType(leaveTypeDTO));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @Operation(summary = "Get a leave type by ID")
    public ResponseEntity<LeaveTypeDTO> getLeaveTypeById(
            @Parameter(description = "ID of the leave type") @PathVariable Long id) {
        return ResponseEntity.ok(leaveTypeService.getLeaveTypeById(id));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update specific fields of a leave type", description = "Update carry-forward settings, accrual rate, or other specific fields of a leave type")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Leave type updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "404", description = "Leave type not found"),
            @ApiResponse(responseCode = "409", description = "Leave type name already exists")
    })
    public ResponseEntity<LeaveTypeDTO> updateLeaveType(
            @Parameter(description = "ID of the leave type") @PathVariable Long id,
            @Valid @RequestBody LeaveTypeUpdateDTO updateDTO) {
        return ResponseEntity.ok(leaveTypeService.updateLeaveType(id, updateDTO));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a leave type", description = "Delete a leave type by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Leave type deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Leave type not found"),
            @ApiResponse(responseCode = "400", description = "Leave type cannot be deleted")
    })
    public ResponseEntity<Void> deleteLeaveType(
            @Parameter(description = "ID of the leave type to delete") @PathVariable Long id) {
        leaveTypeService.deleteLeaveType(id);
        return ResponseEntity.noContent().build();
    }
}
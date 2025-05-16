package com.africa.hr.service;

import com.africa.hr.dto.LeaveRequestDTO;
import com.africa.hr.dto.LeaveRequestApprovalDTO;
import com.africa.hr.model.*;
import com.africa.hr.repository.LeaveRequestRepository;
import com.africa.hr.service.email.EmailService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveRequestServiceTest {

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @Mock
    private LeaveTypeService leaveTypeService;

    @Mock
    private EmployeeBalanceService employeeBalanceService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private LeaveRequestService leaveRequestService;

    private User employee;
    private User manager;
    private Department department;
    private LeaveType leaveType;
    private LeaveRequest leaveRequest;
    private LeaveRequestDTO requestDTO;
    private LeaveRequestApprovalDTO approvalDTO;

    @BeforeEach
    void setUp() {
        department = new Department();
        department.setId(1L);
        department.setName("IT Department");

        manager = new User();
        manager.setId(2L);
        manager.setFirstName("John");
        manager.setLastName("Doe");
        manager.setEmail("john.doe@example.com");
        manager.setDepartment(department);

        // Setup manager
        Role managerRole = new Role();
        managerRole.setName("ROLE_MANAGER");
        manager.setRole(managerRole);

        employee = new User();
        employee.setId(1L);
        employee.setFirstName("Jane");
        employee.setLastName("Smith");
        employee.setEmail("jane.smith@example.com");
        employee.setDepartment(department);
        employee.setManager(manager);

        leaveType = new LeaveType();
        leaveType.setId(1L);
        leaveType.setName("Annual Leave");

        leaveRequest = new LeaveRequest();
        leaveRequest.setId(1L);
        leaveRequest.setEmployee(employee);
        leaveRequest.setLeaveType(leaveType);
        leaveRequest.setStartDate(LocalDate.now().plusDays(1));
        leaveRequest.setEndDate(LocalDate.now().plusDays(5));
        leaveRequest.setStatus(LeaveRequestStatus.PENDING);

        requestDTO = new LeaveRequestDTO();
        requestDTO.setLeaveTypeId(1L);
        requestDTO.setStartDate(LocalDate.now().plusDays(1));
        requestDTO.setEndDate(LocalDate.now().plusDays(5));

        approvalDTO = new LeaveRequestApprovalDTO();
        approvalDTO.setStatus(LeaveRequestStatus.APPROVED);

        // Setup unauthorized manager
        Role unauthorizedManagerRole = new Role();
        unauthorizedManagerRole.setName("ROLE_MANAGER");
        manager.setRole(unauthorizedManagerRole);
    }

    @Test
    void submitLeaveRequest_Success() {
        when(leaveTypeService.getLeaveType(anyLong())).thenReturn(leaveType);
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenReturn(leaveRequest);

        LeaveRequest result = leaveRequestService.submitLeaveRequest(employee, requestDTO);

        assertNotNull(result);
        assertEquals(LeaveRequestStatus.PENDING, result.getStatus());
        verify(emailService).sendLeaveRequestNotification(any(LeaveRequest.class));
    }

    @Test
    void approveLeaveRequest_Success() {
        when(leaveRequestRepository.findById(anyLong())).thenReturn(Optional.of(leaveRequest));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenReturn(leaveRequest);

        LeaveRequest result = leaveRequestService.approveLeaveRequest(1L, manager, approvalDTO);

        assertNotNull(result);
        assertEquals(LeaveRequestStatus.APPROVED, result.getStatus());
        verify(emailService).sendLeaveRequestStatusNotification(any(LeaveRequest.class));
    }

    @Test
    void approveLeaveRequest_UnauthorizedManager() {
        User unauthorizedManager = new User();
        unauthorizedManager.setId(3L);
        unauthorizedManager.setDepartment(new Department());

        when(leaveRequestRepository.findById(anyLong())).thenReturn(Optional.of(leaveRequest));

        assertThrows(IllegalStateException.class,
                () -> leaveRequestService.approveLeaveRequest(1L, unauthorizedManager, approvalDTO));
    }

    @Test
    void getLeaveRequest_NotFound() {
        when(leaveRequestRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> leaveRequestService.getLeaveRequest(1L));
    }

    @Test
    void cancelLeaveRequest_Success() {
        when(leaveRequestRepository.findById(anyLong())).thenReturn(Optional.of(leaveRequest));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenReturn(leaveRequest);

        leaveRequestService.cancelLeaveRequest(1L, employee);

        verify(leaveRequestRepository).save(any(LeaveRequest.class));
    }

    @Test
    void cancelLeaveRequest_UnauthorizedEmployee() {
        User unauthorizedEmployee = new User();
        unauthorizedEmployee.setId(3L);

        when(leaveRequestRepository.findById(anyLong())).thenReturn(Optional.of(leaveRequest));

        assertThrows(IllegalStateException.class,
                () -> leaveRequestService.cancelLeaveRequest(1L, unauthorizedEmployee));
    }
}
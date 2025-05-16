package com.africa.hr.repository;

import com.africa.hr.model.LeaveRequest;
import com.africa.hr.model.LeaveRequestStatus;
import com.africa.hr.model.LeaveType;
import com.africa.hr.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

        @EntityGraph(attributePaths = { "employee", "employee.department", "employee.manager", "leaveType",
                        "manager" })
        @Query("SELECT lr FROM LeaveRequest lr WHERE lr.employee.id = :employeeId")
        List<LeaveRequest> findByEmployeeId(@Param("employeeId") Long employeeId);

        @EntityGraph(attributePaths = { "employee", "employee.department", "employee.manager", "leaveType",
                        "manager" })
        @Query("SELECT lr FROM LeaveRequest lr WHERE lr.employee.id = :employeeId AND lr.status = :status")
        List<LeaveRequest> findByEmployeeIdAndStatus(@Param("employeeId") Long employeeId,
                        @Param("status") LeaveRequestStatus status);

        @EntityGraph(attributePaths = { "employee", "employee.department", "employee.manager", "leaveType",
                        "manager" })
        @Query("SELECT lr FROM LeaveRequest lr WHERE lr.employee.department.id = :departmentId")
        Page<LeaveRequest> findByDepartmentId(@Param("departmentId") Long departmentId, Pageable pageable);

        @EntityGraph(attributePaths = { "employee", "employee.department", "employee.manager", "leaveType",
                        "manager" })
        @Query("SELECT lr FROM LeaveRequest lr WHERE lr.employee.department.id = :departmentId AND lr.status = :status")
        Page<LeaveRequest> findByDepartmentIdAndStatus(@Param("departmentId") Long departmentId,
                        @Param("status") LeaveRequestStatus status, Pageable pageable);

        @Query("SELECT u FROM User u LEFT JOIN FETCH u.manager WHERE u.id = :employeeId")
        Optional<User> findEmployeeWithManager(@Param("employeeId") Long employeeId);

        @EntityGraph(attributePaths = { "employee", "employee.department", "employee.manager", "leaveType",
                        "manager" })
        Page<LeaveRequest> findByEmployee(User employee, Pageable pageable);

        @EntityGraph(attributePaths = { "employee", "employee.department", "employee.manager", "leaveType",
                        "manager" })
        List<LeaveRequest> findByEmployeeAndStatus(User employee, LeaveRequestStatus status);

        @EntityGraph(attributePaths = { "employee", "employee.department", "employee.manager", "leaveType",
                        "manager" })
        List<LeaveRequest> findByEmployeeAndLeaveTypeAndStatus(User employee, LeaveType leaveType,
                        LeaveRequestStatus status);

        @EntityGraph(attributePaths = { "employee", "employee.department", "employee.manager", "leaveType",
                        "manager" })
        @Query("SELECT lr FROM LeaveRequest lr " +
                        "WHERE lr.employee.department.id = :departmentId " +
                        "AND lr.status = :status")
        Page<LeaveRequest> findByDepartmentAndStatus(
                        @Param("departmentId") Long departmentId,
                        @Param("status") LeaveRequestStatus status,
                        Pageable pageable);

        @EntityGraph(attributePaths = { "employee", "employee.department", "employee.manager", "leaveType",
                        "manager" })
        @Query("SELECT lr FROM LeaveRequest lr " +
                        "WHERE lr.employee.department.id = :departmentId " +
                        "AND lr.status = :status " +
                        "AND lr.startDate >= :startDate " +
                        "AND lr.endDate <= :endDate")
        Page<LeaveRequest> findByDepartmentAndStatusAndDateRange(
                        @Param("departmentId") Long departmentId,
                        @Param("status") LeaveRequestStatus status,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        Pageable pageable);

        @Query("SELECT COUNT(lr) FROM LeaveRequest lr " +
                        "WHERE lr.employee = :employee " +
                        "AND lr.leaveType = :leaveTypeId " +
                        "AND lr.status = 'APPROVED' " +
                        "AND lr.startDate >= :startDate " +
                        "AND lr.endDate <= :endDate")
        long countApprovedLeaveRequests(
                        @Param("employee") User employee,
                        @Param("leaveTypeId") Long leaveTypeId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        @EntityGraph(attributePaths = { "employee", "employee.department", "employee.manager", "leaveType",
                        "manager" })
        @Query("SELECT lr FROM LeaveRequest lr " +
                        "WHERE lr.employee.department.id = :departmentId " +
                        "AND ((lr.startDate BETWEEN :startDate AND :endDate) " +
                        "OR (lr.endDate BETWEEN :startDate AND :endDate) " +
                        "OR (:startDate BETWEEN lr.startDate AND lr.endDate))")
        List<LeaveRequest> findByDepartmentAndDateRange(
                        @Param("departmentId") Long departmentId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        @EntityGraph(attributePaths = { "employee", "employee.department", "employee.manager", "leaveType",
                        "manager" })
        @Query("SELECT lr FROM LeaveRequest lr " +
                        "WHERE (lr.startDate BETWEEN :startDate AND :endDate) " +
                        "OR (lr.endDate BETWEEN :startDate AND :endDate) " +
                        "OR (:startDate BETWEEN lr.startDate AND lr.endDate)")
        List<LeaveRequest> findByDateRange(
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        @Query("SELECT lr FROM LeaveRequest lr " +
                        "JOIN FETCH lr.employee e " +
                        "JOIN FETCH e.department " +
                        "LEFT JOIN FETCH e.manager m " +
                        "LEFT JOIN FETCH m.department " +
                        "LEFT JOIN FETCH m.role " +
                        "JOIN FETCH e.role " +
                        "JOIN FETCH lr.leaveType " +
                        "LEFT JOIN FETCH lr.manager a " +
                        "LEFT JOIN FETCH a.department " +
                        "LEFT JOIN FETCH a.role " +
                        "WHERE lr.id = :id")
        Optional<LeaveRequest> findByIdWithAllAssociations(@Param("id") Long id);

        @Override
        @EntityGraph(attributePaths = { "employee", "employee.department", "employee.manager", "leaveType",
                        "manager" })
        Optional<LeaveRequest> findById(Long id);

        @EntityGraph(attributePaths = { "employee", "employee.department", "employee.manager", "leaveType",
                        "manager" })
        @Query("SELECT lr FROM LeaveRequest lr WHERE lr.id = :id")
        Optional<LeaveRequest> findWithDetailsById(@Param("id") Long id);

        @EntityGraph(attributePaths = { "employee", "employee.department", "leaveType", "manager" })
        @Query("SELECT lr FROM LeaveRequest lr WHERE lr.manager.id = :managerId")
        Page<LeaveRequest> findByManagerId(@Param("managerId") Long managerId, Pageable pageable);

        @EntityGraph(attributePaths = { "employee", "employee.department", "leaveType", "manager" })
        @Query("SELECT lr FROM LeaveRequest lr WHERE lr.manager.id = :managerId AND lr.status = :status")
        Page<LeaveRequest> findByManagerIdAndStatus(@Param("managerId") Long managerId,
                        @Param("status") LeaveRequestStatus status, Pageable pageable);
}
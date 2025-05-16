package com.africa.hr.repository;

import com.africa.hr.model.EmployeeBalance;
import com.africa.hr.model.LeaveType;
import com.africa.hr.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeBalanceRepository extends JpaRepository<EmployeeBalance, Long> {

    /**
     * Find all balances for a specific employee.
     *
     * @param employee the employee to find balances for
     * @return list of employee balances
     */
    List<EmployeeBalance> findByEmployee(User employee);

    /**
     * Find a specific balance for an employee and leave type.
     *
     * @param employee  the employee
     * @param leaveType the leave type
     * @return optional containing the balance if found
     */
    Optional<EmployeeBalance> findByEmployeeAndLeaveType(User employee, LeaveType leaveType);

    /**
     * Find all balances that are eligible for accrual and haven't been updated
     * since a specific date.
     *
     * @param date the date to check against
     * @return list of eligible balances
     */
    @Query("SELECT eb FROM EmployeeBalance eb " +
            "WHERE eb.isEligibleForAccrual = true " +
            "AND (eb.lastAccrualDate IS NULL OR eb.lastAccrualDate < :date) " +
            "AND eb.employee.status = 'ACTIVE' " +
            "AND eb.leaveType.accrualBased = true")
    List<EmployeeBalance> findEligibleForAccrual(@Param("date") LocalDate date);

    /**
     * Find all balances that are eligible for carry-forward processing.
     *
     * @return list of eligible balances
     */
    @Query("SELECT eb FROM EmployeeBalance eb " +
            "WHERE eb.leaveType.isCarryForwardEnabled = true " +
            "AND eb.currentBalance > :zero " +
            "AND eb.employee.status = 'ACTIVE'")
    List<EmployeeBalance> findEligibleForCarryForward(@Param("zero") BigDecimal zero);

    /**
     * Find all balances for employees who joined after a specific date.
     *
     * @param date the date to check against
     * @return list of balances for new employees
     */
    @Query("SELECT eb FROM EmployeeBalance eb " +
            "WHERE eb.employee.joinedDate > :date " +
            "AND eb.employee.status = 'ACTIVE'")
    List<EmployeeBalance> findByEmployeeJoinedAfter(@Param("date") LocalDate date);

    /**
     * Find all balances that exceed their maximum balance.
     *
     * @return list of balances that exceed their maximum
     */
    @Query("SELECT eb FROM EmployeeBalance eb " +
            "WHERE eb.maxBalance IS NOT NULL " +
            "AND eb.currentBalance > eb.maxBalance")
    List<EmployeeBalance> findExceedingMaxBalance();

    /**
     * Check if an employee has any balance for a specific leave type.
     *
     * @param employee  the employee to check
     * @param leaveType the leave type to check
     * @return true if a balance exists
     */
    boolean existsByEmployeeAndLeaveType(User employee, LeaveType leaveType);

    /**
     * Find all employee balances for a list of leave types.
     *
     * @param leaveTypes the list of leave types
     * @return list of employee balances
     */
    List<EmployeeBalance> findByLeaveTypeIn(List<LeaveType> leaveTypes);
}
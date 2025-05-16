package com.africa.hr.repository;

import com.africa.hr.model.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeaveTypeRepository extends JpaRepository<LeaveType, Long> {

    /**
     * Find a leave type by its name.
     *
     * @param name the name of the leave type
     * @return optional containing the leave type if found
     */
    Optional<LeaveType> findByName(String name);

    /**
     * Find all default leave types.
     *
     * @return list of default leave types
     */
    List<LeaveType> findByIsDefaultTrue();

    /**
     * Find all accrual-based leave types.
     *
     * @return list of accrual-based leave types
     */
    List<LeaveType> findByAccrualBasedTrue();

    /**
     * Find all leave types that have carry-forward enabled.
     *
     * @return list of leave types with carry-forward enabled
     */
    List<LeaveType> findByIsCarryForwardEnabledTrue();

    /**
     * Find all paid leave types.
     *
     * @return list of paid leave types
     */
    List<LeaveType> findByPaidTrue();

    /**
     * Check if a leave type with the given name exists.
     *
     * @param name the name to check
     * @return true if a leave type with the name exists
     */
    boolean existsByName(String name);

    /**
     * Find all leave types that are eligible for accrual processing.
     * This includes leave types that are accrual-based and have a valid accrual
     * rate.
     *
     * @return list of eligible leave types
     */
    @Query("SELECT lt FROM LeaveType lt " +
            "WHERE lt.accrualBased = true " +
            "AND lt.accrualRate IS NOT NULL " +
            "AND lt.accrualRate > 0")
    List<LeaveType> findEligibleForAccrual();

    /**
     * Find all leave types that are eligible for carry-forward processing.
     * This includes leave types that have carry-forward enabled and have a valid
     * carry-forward cap.
     *
     * @return list of eligible leave types
     */
    @Query("SELECT lt FROM LeaveType lt " +
            "WHERE lt.isCarryForwardEnabled = true " +
            "AND lt.carryForwardCap IS NOT NULL " +
            "AND lt.carryForwardCap > 0")
    List<LeaveType> findEligibleForCarryForward();
}
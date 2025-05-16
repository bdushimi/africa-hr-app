package com.africa.hr.repository;

import com.africa.hr.model.EmployeeBalance;
import com.africa.hr.model.LeaveCarryForward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LeaveCarryForwardRepository extends JpaRepository<LeaveCarryForward, Long> {

        /**
         * Find all carry-forwards for an employee balance, ordered by carry-forward
         * date descending.
         *
         * @param employeeBalance the employee balance
         * @return list of carry-forwards
         */
        List<LeaveCarryForward> findByEmployeeBalanceOrderByCarryForwardDateDesc(EmployeeBalance employeeBalance);

        /**
         * Find carry-forward for an employee balance for a specific year transition.
         *
         * @param employeeBalance the employee balance
         * @param fromYear        the year to carry forward from
         * @param toYear          the year to carry forward to
         * @return optional containing the carry-forward if found
         */
        Optional<LeaveCarryForward> findByEmployeeBalanceAndFromYearAndToYear(
                        EmployeeBalance employeeBalance,
                        Integer fromYear,
                        Integer toYear);

        /**
         * Find all carry-forwards processed on a specific date.
         *
         * @param date the date to check
         * @return list of carry-forwards
         */
        List<LeaveCarryForward> findByCarryForwardDate(LocalDate date);

        /**
         * Find all carry-forwards for a specific year transition.
         *
         * @param fromYear the year to carry forward from
         * @param toYear   the year to carry forward to
         * @return list of carry-forwards
         */
        List<LeaveCarryForward> findByFromYearAndToYear(Integer fromYear, Integer toYear);

        /**
         * Get the total carried forward amount for an employee balance in a specific
         * year transition.
         *
         * @param employeeBalance the employee balance
         * @param fromYear        the year to carry forward from
         * @param toYear          the year to carry forward to
         * @return the total carried forward amount
         */
        @Query("SELECT SUM(lcf.carriedForwardAmount) FROM LeaveCarryForward lcf " +
                        "WHERE lcf.employeeBalance = :employeeBalance " +
                        "AND lcf.fromYear = :fromYear " +
                        "AND lcf.toYear = :toYear")
        BigDecimal getTotalCarriedForwardForYearTransition(
                        @Param("employeeBalance") EmployeeBalance employeeBalance,
                        @Param("fromYear") Integer fromYear,
                        @Param("toYear") Integer toYear);

        /**
         * Find carry-forwards for an employee balance from a specific year.
         *
         * @param balance  the employee balance
         * @param fromYear the year to find carry-forwards from
         * @return list of carry-forwards
         */
        List<LeaveCarryForward> findByEmployeeBalanceAndFromYear(EmployeeBalance balance, int fromYear);
}
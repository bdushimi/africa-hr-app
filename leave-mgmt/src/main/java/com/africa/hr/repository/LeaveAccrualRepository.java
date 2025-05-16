package com.africa.hr.repository;

import com.africa.hr.model.EmployeeBalance;
import com.africa.hr.model.LeaveAccrual;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

@Repository
public interface LeaveAccrualRepository extends JpaRepository<LeaveAccrual, Long> {

        /**
         * Find all accruals for an employee balance, ordered by accrual date
         * descending.
         *
         * @param employeeBalance the employee balance
         * @return list of accruals
         */
        List<LeaveAccrual> findByEmployeeBalanceOrderByAccrualDateDesc(EmployeeBalance employeeBalance);

        /**
         * Find all accruals for a specific year and month.
         *
         * @param yearMonth the year and month
         * @return list of accruals
         */
        List<LeaveAccrual> findByYearMonth(YearMonth yearMonth);

        /**
         * Find all accruals for an employee balance in a specific year.
         *
         * @param employeeBalance the employee balance
         * @param year            the year
         * @return list of accruals
         */
        @Query("SELECT la FROM LeaveAccrual la " +
                        "WHERE la.employeeBalance = :employeeBalance " +
                        "AND FUNCTION('YEAR', la.accrualDate) = :year " +
                        "ORDER BY FUNCTION('MONTH', la.accrualDate) ASC")
        List<LeaveAccrual> findByEmployeeBalanceAndYearMonthYear(
                        @Param("employeeBalance") EmployeeBalance employeeBalance,
                        @Param("year") int year);

        /**
         * Get the total accrued amount for an employee balance in a specific year.
         *
         * @param employeeBalance the employee balance
         * @param year            the year
         * @return the total accrued amount
         */
        @Query("SELECT SUM(la.amount) FROM LeaveAccrual la " +
                        "WHERE la.employeeBalance = :employeeBalance " +
                        "AND FUNCTION('YEAR', la.accrualDate) = :year")
        BigDecimal getTotalAccruedForYear(
                        @Param("employeeBalance") EmployeeBalance employeeBalance,
                        @Param("year") int year);

        /**
         * Find accruals for an employee balance in a specific year, ordered by month
         * descending.
         *
         * @param employeeBalance the employee balance
         * @param year            the year to find accruals for
         * @return list of accruals
         */
        @Query("SELECT la FROM LeaveAccrual la " +
                        "WHERE la.employeeBalance = :employeeBalance " +
                        "AND FUNCTION('YEAR', la.accrualDate) = :year " +
                        "ORDER BY FUNCTION('MONTH', la.accrualDate) DESC")
        List<LeaveAccrual> findByEmployeeBalanceAndAccrualYearOrderByAccrualMonthDesc(
                        @Param("employeeBalance") EmployeeBalance employeeBalance,
                        @Param("year") Integer year);

        /**
         * Find accruals for an employee balance in a specific year and month.
         *
         * @param employeeBalance the employee balance
         * @param year            the year to find accruals for
         * @param month           the month to find accruals for
         * @return list of accruals
         */
        @Query("SELECT la FROM LeaveAccrual la " +
                        "WHERE la.employeeBalance = :employeeBalance " +
                        "AND FUNCTION('YEAR', la.accrualDate) = :year " +
                        "AND FUNCTION('MONTH', la.accrualDate) = :month")
        List<LeaveAccrual> findByEmployeeBalanceAndAccrualYearAndAccrualMonth(
                        @Param("employeeBalance") EmployeeBalance employeeBalance,
                        @Param("year") Integer year,
                        @Param("month") Integer month);

        /**
         * Find all accruals for a specific year and month.
         *
         * @param year  the year to find accruals for
         * @param month the month to find accruals for
         * @return list of accruals
         */
        @Query("SELECT la FROM LeaveAccrual la " +
                        "WHERE FUNCTION('YEAR', la.accrualDate) = :year " +
                        "AND FUNCTION('MONTH', la.accrualDate) = :month")
        List<LeaveAccrual> findByAccrualYearAndAccrualMonth(
                        @Param("year") Integer year,
                        @Param("month") Integer month);
}
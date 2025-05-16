package com.africa.hr.repository;

import com.africa.hr.model.PublicHoliday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PublicHolidayRepository extends JpaRepository<PublicHoliday, Long> {

    /**
     * Find all holidays within a specific date range
     *
     * @param startDate the start date (inclusive)
     * @param endDate   the end date (inclusive)
     * @return list of public holidays
     */
    List<PublicHoliday> findByDateBetweenOrderByDateAsc(LocalDate startDate, LocalDate endDate);

    /**
     * Find a holiday on a specific date
     *
     * @param date the date to search for
     * @return the public holiday, if it exists
     */
    PublicHoliday findByDate(LocalDate date);

    /**
     * Find all holidays in a specific year
     *
     * @param year the year to search in
     * @return list of public holidays
     */
    @Query("SELECT ph FROM PublicHoliday ph WHERE FUNCTION('YEAR', ph.date) = :year ORDER BY ph.date ASC")
    List<PublicHoliday> findByYear(@Param("year") int year);

    /**
     * Check if a specific date is a holiday
     *
     * @param date the date to check
     * @return true if the date is a holiday
     */
    boolean existsByDate(LocalDate date);

    /**
     * Find all recurring holidays
     *
     * @return list of recurring holidays
     */
    List<PublicHoliday> findByIsRecurringTrue();
}
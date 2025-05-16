package com.africa.hr.service;

import com.africa.hr.model.PublicHoliday;
import com.africa.hr.repository.PublicHolidayRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing public holidays.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PublicHolidayService {

    private final PublicHolidayRepository publicHolidayRepository;

    /**
     * Get a list of all public holidays
     *
     * @return a list of all public holidays
     */
    @Transactional(readOnly = true)
    public List<PublicHoliday> getAllHolidays() {
        return publicHolidayRepository.findAll();
    }

    /**
     * Get a public holiday by ID
     *
     * @param id the ID of the public holiday
     * @return the public holiday
     * @throws EntityNotFoundException if the holiday is not found
     */
    @Transactional(readOnly = true)
    public PublicHoliday getHolidayById(Long id) {
        return publicHolidayRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Public holiday not found with ID: " + id));
    }

    /**
     * Get public holidays within a date range, including recurring holidays from
     * previous years
     *
     * @param startDate the start date (inclusive)
     * @param endDate   the end date (inclusive)
     * @return a list of public holidays within the date range
     */
    @Transactional(readOnly = true)
    public List<PublicHoliday> getHolidaysBetweenDates(LocalDate startDate, LocalDate endDate) {
        log.info("Fetching public holidays between {} and {}", startDate, endDate);

        // Get exact date matches from the database
        List<PublicHoliday> directMatches = publicHolidayRepository.findByDateBetweenOrderByDateAsc(startDate, endDate);
        log.info("Found {} direct holiday matches", directMatches.size());

        // For recurring holidays, we need to check if any holidays from previous years
        // should be included
        // We'll get all recurring holidays
        List<PublicHoliday> allRecurringHolidays = publicHolidayRepository.findByIsRecurringTrue();
        log.info("Found {} recurring holidays in total", allRecurringHolidays.size());

        // Create a list to track dates we've already covered with direct matches
        List<LocalDate> coveredDates = directMatches.stream()
                .map(PublicHoliday::getDate)
                .collect(Collectors.toList());

        // List to store holidays to add (recurring holidays for the current year)
        List<PublicHoliday> additionalHolidays = new ArrayList<>();

        // Loop through recurring holidays and add them if they fall within our date
        // range
        for (PublicHoliday recurringHoliday : allRecurringHolidays) {
            // For each recurring holiday, create a version for the requested year if it's
            // within our range
            LocalDate originalDate = recurringHoliday.getDate();

            // For each year in the range
            for (int year = startDate.getYear(); year <= endDate.getYear(); year++) {
                // Create a date with the same month and day but for the current year
                LocalDate projectedDate = LocalDate.of(year, originalDate.getMonth(), originalDate.getDayOfMonth());

                // Check if this date is within our range and not already covered
                if (!coveredDates.contains(projectedDate) &&
                        !projectedDate.isBefore(startDate) &&
                        !projectedDate.isAfter(endDate)) {

                    // Create a copy of the holiday with the projected date
                    PublicHoliday projectedHoliday = new PublicHoliday();
                    projectedHoliday.setId(recurringHoliday.getId());
                    projectedHoliday.setName(recurringHoliday.getName());
                    projectedHoliday.setDate(projectedDate);
                    projectedHoliday.setDescription(recurringHoliday.getDescription());
                    projectedHoliday.setIsRecurring(true);

                    additionalHolidays.add(projectedHoliday);
                    coveredDates.add(projectedDate); // Mark this date as covered
                }
            }
        }

        log.info("Adding {} recurring holidays projected to the requested date range", additionalHolidays.size());

        // Combine direct matches and recurring holidays
        List<PublicHoliday> allHolidays = new ArrayList<>(directMatches);
        allHolidays.addAll(additionalHolidays);

        // Sort by date
        allHolidays.sort(Comparator.comparing(PublicHoliday::getDate));

        log.info("Returning a total of {} holidays", allHolidays.size());
        return allHolidays;
    }

    /**
     * Get public holidays for a specific year
     *
     * @param year the year to get holidays for
     * @return a list of public holidays in the specified year
     */
    @Transactional(readOnly = true)
    public List<PublicHoliday> getHolidaysByYear(int year) {
        return publicHolidayRepository.findByYear(year);
    }

    /**
     * Check if a specific date is a public holiday
     *
     * @param date the date to check
     * @return true if the date is a public holiday, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean isPublicHoliday(LocalDate date) {
        return publicHolidayRepository.existsByDate(date);
    }

    /**
     * Create a new public holiday
     *
     * @param holiday the public holiday to create
     * @return the created public holiday
     */
    @Transactional
    public PublicHoliday createHoliday(PublicHoliday holiday) {
        log.info("Creating new public holiday: {}", holiday.getName());
        return publicHolidayRepository.save(holiday);
    }

    /**
     * Update an existing public holiday
     *
     * @param id             the ID of the public holiday to update
     * @param updatedHoliday the updated public holiday data
     * @return the updated public holiday
     * @throws EntityNotFoundException if the holiday is not found
     */
    @Transactional
    public PublicHoliday updateHoliday(Long id, PublicHoliday updatedHoliday) {
        log.info("Updating public holiday with ID: {}", id);

        PublicHoliday existingHoliday = publicHolidayRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Public holiday not found with ID: " + id));

        existingHoliday.setName(updatedHoliday.getName());
        existingHoliday.setDate(updatedHoliday.getDate());
        existingHoliday.setDescription(updatedHoliday.getDescription());
        existingHoliday.setIsRecurring(updatedHoliday.getIsRecurring());

        return publicHolidayRepository.save(existingHoliday);
    }

    /**
     * Delete a public holiday
     *
     * @param id the ID of the public holiday to delete
     * @throws EntityNotFoundException if the holiday is not found
     */
    @Transactional
    public void deleteHoliday(Long id) {
        log.info("Deleting public holiday with ID: {}", id);

        if (!publicHolidayRepository.existsById(id)) {
            throw new EntityNotFoundException("Public holiday not found with ID: " + id);
        }

        publicHolidayRepository.deleteById(id);
    }
}
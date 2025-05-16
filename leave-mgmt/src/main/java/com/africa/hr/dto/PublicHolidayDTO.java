package com.africa.hr.dto;

import com.africa.hr.model.PublicHoliday;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for public holiday information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicHolidayDTO {
    private Long id;
    private String name;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    private String description;

    /**
     * Convert a PublicHoliday entity to a PublicHolidayDTO.
     *
     * @param holiday the PublicHoliday entity
     * @return the PublicHolidayDTO
     */
    public static PublicHolidayDTO fromEntity(PublicHoliday holiday) {
        return PublicHolidayDTO.builder()
                .id(holiday.getId())
                .name(holiday.getName())
                .date(holiday.getDate())
                .description(holiday.getDescription())
                .build();
    }
}
package com.africa.hr.dto;

import lombok.Data;

@Data
public class LeaveTypeStatsDTO {
    private String type;
    private Double total;
    private Double used;
    private Double daysLeft;
}
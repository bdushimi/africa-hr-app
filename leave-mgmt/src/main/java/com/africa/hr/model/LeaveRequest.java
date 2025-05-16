package com.africa.hr.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "leave_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaveRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Employee is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private User employee;

    @NotNull(message = "Leave type is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leave_type_id", nullable = false)
    private LeaveType leaveType;

    @NotNull(message = "Start date is required")
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "half_day_start", nullable = false)
    private Boolean halfDayStart = false;

    @Column(name = "half_day_end", nullable = false)
    private Boolean halfDayEnd = false;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeaveRequestStatus status = LeaveRequestStatus.PENDING;

    @Column(name = "leave_request_reason", length = 1000)
    private String leaveRequestReason;

    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    @OneToMany(mappedBy = "leaveRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<Document> documents = new java.util.ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private User manager;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "primary_document_id")
    private Document primaryDocument;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Calculates the total number of days requested, taking into account half days.
     * The calculation includes both start and end dates.
     * 
     * @return the total number of days requested
     */
    public double getTotalDaysRequested() {
        // Calculate base days (including both start and end dates)
        long baseDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;

        // Adjust for half days
        if (Boolean.TRUE.equals(halfDayStart)) {
            baseDays -= 0.5;
        }
        if (Boolean.TRUE.equals(halfDayEnd)) {
            baseDays -= 0.5;
        }

        return baseDays;
    }
}
package com.example.HealthcareSystem_Backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "leave_records")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Enumerated(EnumType.STRING)
    private LeaveType leaveType;

    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private LeaveStatus status = LeaveStatus.PENDING;

    private String approvedBy;

    public enum LeaveType {
        CASUAL, SICK, ANNUAL, MATERNITY, EMERGENCY
    }

    public enum LeaveStatus {
        PENDING, APPROVED, REJECTED
    }
}
package com.example.HealthcareSystem_Backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "staff_attendance", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "employee_id", "attendance_date" })
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffAttendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    private LocalTime checkInTime;
    private LocalTime checkOutTime;

    @Enumerated(EnumType.STRING)
    private AttendanceStatus status;

    public enum AttendanceStatus {
        PRESENT, LATE, HALF_DAY, ABSENT
    }
}
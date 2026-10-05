package com.example.HealthcareSystem_Backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "employees")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String employeeCode;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    private String phone;
    private String designation;

    @ManyToOne
    @JoinColumn(name = "department_id")
    private Department department;

    private LocalDate dateOfJoining;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private EmploymentStatus status = EmploymentStatus.ACTIVE;

    // Added field to satisfy database NOT NULL constraint
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    public enum EmploymentStatus {
        ACTIVE, ON_LEAVE, RESIGNED, TERMINATED
    }
}
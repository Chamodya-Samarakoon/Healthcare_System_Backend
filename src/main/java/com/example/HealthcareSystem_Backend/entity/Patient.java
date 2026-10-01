package com.example.HealthcareSystem_Backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "patients")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Patient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String patientNumber; // e.g. PAT-1001

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    private LocalDate dateOfBirth;
    private String gender;

    @Column(unique = true)
    private String nic;

    private String phone;
    private String email;
    private String address;
    private String emergencyContact;
    private String bloodGroup;
    private String conditionSummary;
    private LocalDate registrationDate;
}